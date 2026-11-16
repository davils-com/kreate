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

package com.davils.kreate.functional

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class RecordingMavenServer : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    val uploads: MutableList<String> = CopyOnWriteArrayList()

    val authorizations: MutableList<String> = CopyOnWriteArrayList()

    val address: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange -> answer(exchange) }
        server.start()
    }

    override fun close() {
        server.stop(0)
    }

    private fun answer(exchange: HttpExchange) {
        exchange.use { open ->
            val status = statusOf(open)
            open.requestBody.readAllBytes()
            open.sendResponseHeaders(status, NO_BODY)
        }
    }

    private fun statusOf(exchange: HttpExchange): Int {
        if (exchange.requestMethod != UPLOAD_METHOD) return NOT_FOUND

        uploads += exchange.requestURI.path
        val authorization = exchange.requestHeaders.getFirst(AUTHORIZATION_HEADER)
        authorizations += authorization.orEmpty()
        return CREATED
    }

    private companion object {
        const val UPLOAD_METHOD: String = "PUT"
        const val AUTHORIZATION_HEADER: String = "Authorization"
        const val CREATED: Int = 201
        const val NOT_FOUND: Int = 404
        const val NO_BODY: Long = -1
    }
}
