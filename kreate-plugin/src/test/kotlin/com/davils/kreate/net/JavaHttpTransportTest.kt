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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.IOException
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration

private val TIMEOUT: Duration = Duration.ofSeconds(10)

private fun transport(timeout: Duration = TIMEOUT): JavaHttpTransport {
    val builder = HttpClient.newBuilder()
    val client = builder.followRedirects(HttpClient.Redirect.NEVER).build()
    return JavaHttpTransport(client, "kreate-test/1.0", timeout, FIXED_CLOCK)
}

class JavaHttpTransportTest : FunSpec({
    val server = autoClose(LoopbackServer.http())

    test("returns status, headers, body and the time of arrival") {
        server.respond("/ok", 200, mapOf("X-Thing" to "1"), "hello".toByteArray())

        val response = transport().send(TransportRequest(HttpMethod.GET, server.uri("/ok")))

        response.statusCode shouldBe 200
        response.header("x-thing") shouldBe "1"
        response.text shouldBe "hello"
        response.uri shouldBe server.uri("/ok")
        response.receivedAt shouldBe FIXED_INSTANT
    }

    test("identifies itself and sends the request headers") {
        server.respond("/headers")

        transport().send(TransportRequest(HttpMethod.GET, server.uri("/headers"), mapOf("Accept" to "text/plain")))

        val request = server.requests.last { it.path == "/headers" }
        request.header("User-Agent") shouldBe "kreate-test/1.0"
        request.header("Accept") shouldBe "text/plain"
    }

    test("does not ask for compressed bodies, which would bypass the byte cap") {
        server.respond("/plain")

        transport().send(TransportRequest(HttpMethod.GET, server.uri("/plain")))

        server.requests.last { it.path == "/plain" }.header("Accept-Encoding") shouldBe null
    }

    test("posts a body") {
        server.respond("/post", 201)
        val body = """{"a":1}""".toByteArray()

        val response = transport().send(TransportRequest(HttpMethod.POST, server.uri("/post"), body = body))

        response.statusCode shouldBe 201
        val request = server.requests.last { it.path == "/post" }
        request.method shouldBe "POST"
        request.body shouldBe body
    }

    test("does not follow a redirect itself") {
        server.redirect("/redirect", "/elsewhere")

        val response = transport().send(TransportRequest(HttpMethod.GET, server.uri("/redirect")))

        response.statusCode shouldBe 302
    }

    test("returns error statuses instead of failing") {
        server.respond("/teapot", 418)

        val response = transport().send(TransportRequest(HttpMethod.GET, server.uri("/teapot")))

        response.statusCode shouldBe 418
    }

    context("the byte cap") {
        test("accepts a body of exactly the limit") {
            server.respond("/exact", body = ByteArray(16))

            val request = TransportRequest(HttpMethod.GET, server.uri("/exact"), maxBytes = 16)

            transport().send(request).body shouldHaveSize 16
        }

        test("refuses an oversized body by its declared length") {
            server.respond("/declared", body = ByteArray(17))

            val request = TransportRequest(HttpMethod.GET, server.uri("/declared"), maxBytes = 16)

            shouldThrow<NetworkException.TooLarge> { transport().send(request) }
        }

        test("refuses an oversized body that declares no length") {
            server.handle("/chunked") { exchange ->
                exchange.sendResponseHeaders(200, 0)
                exchange.responseBody.write(ByteArray(17))
            }

            val request = TransportRequest(HttpMethod.GET, server.uri("/chunked"), maxBytes = 16)

            shouldThrow<NetworkException.TooLarge> { transport().send(request) }
        }

        test("stops reading an endless body at the limit") {
            server.handle("/endless") { exchange ->
                exchange.sendResponseHeaders(200, 0)
                val chunk = ByteArray(64 * 1024)
                ignoringDisconnect { while (true) exchange.responseBody.write(chunk) }
            }

            val request = TransportRequest(HttpMethod.GET, server.uri("/endless"), maxBytes = 1024 * 1024)

            shouldThrow<NetworkException.TooLarge> { transport().send(request) }
        }

        test("does not apply the declared length of a HEAD response") {
            server.handle("/head") { exchange ->
                exchange.responseHeaders.add("Content-Length", "1000000")
                exchange.sendResponseHeaders(200, -1)
            }

            val request = TransportRequest(HttpMethod.HEAD, server.uri("/head"), maxBytes = 0)

            val response = transport().send(request)

            response.statusCode shouldBe 200
            response.body.toList().shouldBeEmpty()
        }
    }

    context("failures") {
        test("reports a refused connection as unavailable") {
            val unusedPort = ServerSocket(0).use { socket -> socket.localPort }
            val request = TransportRequest(HttpMethod.GET, URI("http://127.0.0.1:$unusedPort/"))

            shouldThrow<NetworkException.Unavailable> { transport().send(request) }
        }

        test("times out while waiting for the response headers") {
            server.handle("/silent") { ignoringDisconnect { Thread.sleep(5_000) } }

            val request = TransportRequest(HttpMethod.GET, server.uri("/silent"))

            shouldThrow<NetworkException.Unavailable> { transport(Duration.ofMillis(200)).send(request) }
        }

        test("times out a body that trickles forever") {
            server.handle("/slow") { exchange ->
                exchange.sendResponseHeaders(200, 0)
                ignoringDisconnect {
                    while (true) {
                        exchange.responseBody.write(1)
                        exchange.responseBody.flush()
                        Thread.sleep(20)
                    }
                }
            }

            val start = System.nanoTime()
            val failure = shouldThrow<NetworkException.Unavailable> {
                transport(Duration.ofMillis(300)).send(TransportRequest(HttpMethod.GET, server.uri("/slow")))
            }

            failure.message.orEmpty() shouldContain "cut off"
            Duration.ofNanos(System.nanoTime() - start).toSeconds() shouldBeLessThan 10L
        }

        test("reports a body that breaks off") {
            server.handle("/broken") { exchange ->
                exchange.sendResponseHeaders(200, 100)
                exchange.responseBody.write(ByteArray(10))
                exchange.responseBody.flush()
                exchange.close()
            }

            val request = TransportRequest(HttpMethod.GET, server.uri("/broken"))

            shouldThrow<NetworkException.Unavailable> { transport().send(request) }
        }

        test("keeps the interrupt of the calling thread") {
            server.handle("/interrupted") { ignoringDisconnect { Thread.sleep(5_000) } }
            val request = TransportRequest(HttpMethod.GET, server.uri("/interrupted"))

            Thread.currentThread().interrupt()
            shouldThrow<NetworkException.Interrupted> { transport().send(request) }

            Thread.interrupted() shouldBe true
        }
    }
})

private fun ignoringDisconnect(work: () -> Unit) {
    try {
        work()
    } catch (expected: IOException) {
        return
    } catch (expected: InterruptedException) {
        return
    }
}
