/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.net

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val LOOPBACK_HOST: String = "127.0.0.1"

private const val NO_BODY: Long = -1

class LoopbackServer private constructor(
    private val server: HttpServer,
    private val scheme: String
) : AutoCloseable {
    private val executor: ExecutorService = Executors.newCachedThreadPool()

    private val recorded: MutableList<RecordedRequest> = CopyOnWriteArrayList()

    val requests: List<RecordedRequest>
        get() = recorded

    init {
        server.executor = executor
        server.start()
    }

    fun uri(path: String): URI = URI("$scheme://$LOOPBACK_HOST:${server.address.port}$path")

    fun handle(path: String, handler: (HttpExchange) -> Unit) {
        server.createContext(path) { exchange ->
            exchange.use { open ->
                record(open)
                handler(open)
            }
        }
    }

    fun respond(
        path: String,
        status: Int = HTTP_OK,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray = ByteArray(0)
    ) {
        handle(path) { exchange -> exchange.reply(status, headers, body) }
    }

    fun respondInTurn(path: String, vararg statuses: Int) {
        val remaining = ArrayDeque(statuses.asList())
        handle(path) { exchange ->
            val status = synchronized(remaining) { remaining.removeFirst() }
            exchange.reply(status)
        }
    }

    fun redirect(path: String, location: String, status: Int = HTTP_FOUND) {
        respond(path, status, mapOf("Location" to location))
    }

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun record(exchange: HttpExchange) {
        val body = exchange.requestBody.readAllBytes()
        val path = exchange.requestURI.toString()
        recorded.add(RecordedRequest(exchange.requestMethod, path, exchange.requestHeaders.toMap(), body))
    }

    companion object {
        const val HTTP_OK: Int = 200

        const val HTTP_FOUND: Int = 302

        fun http(): LoopbackServer {
            val server = HttpServer.create(loopbackAddress(), 0)
            return LoopbackServer(server, "http")
        }

        fun https(): LoopbackServer {
            val server = HttpsServer.create(loopbackAddress(), 0)
            server.httpsConfigurator = HttpsConfigurator(LoopbackCertificate.serverContext)
            return LoopbackServer(server, "https")
        }

        fun HttpExchange.reply(
            status: Int,
            headers: Map<String, String> = emptyMap(),
            body: ByteArray = ByteArray(0)
        ) {
            headers.forEach { (name, value) -> responseHeaders.add(name, value) }
            val length = body.size.toLong().takeIf { size -> size > 0 } ?: NO_BODY
            sendResponseHeaders(status, length)
            if (body.isNotEmpty()) responseBody.write(body)
        }

        private fun loopbackAddress(): InetSocketAddress =
            InetSocketAddress(InetAddress.getByName(LOOPBACK_HOST), 0)
    }
}
