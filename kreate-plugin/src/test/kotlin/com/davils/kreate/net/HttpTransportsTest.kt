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
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.URI
import java.time.Duration
import kotlin.random.Random

private const val USER_AGENT: String = "kreate-test/1.0"

private fun settings(
    offline: Boolean = false,
    timeout: Duration = Duration.ofSeconds(10),
    proxy: ProxySettings = ProxySettings(),
    caCertificates: java.nio.file.Path? = null
): NetworkSettings = NetworkSettings(USER_AGENT, offline, timeout, proxy, caCertificates)

class HttpTransportsTest : FunSpec({
    val plain = autoClose(LoopbackServer.http())
    val secure = autoClose(LoopbackServer.https())

    fun transport(
        settings: NetworkSettings = settings(),
        sleeper: Sleeper = RecordingSleeper()
    ): HttpTransport = HttpTransports.create(settings, FIXED_CLOCK, sleeper, RetryPolicy(random = Random(1)))

    context("offline mode") {
        test("refuses before connecting") {
            plain.respond("/offline")
            val requestsBefore = plain.requests.size

            val failure = shouldThrow<NetworkException.Offline> {
                transport(settings(offline = true)).send(TransportRequest(HttpMethod.GET, plain.uri("/offline")))
            }

            failure.message.orEmpty() shouldContain "offline"
            plain.requests shouldHaveSize requestsBefore
        }
    }

    context("redirects") {
        test("follows a redirect by hand") {
            plain.redirect("/from", "/to")
            plain.respond("/to", body = "arrived".toByteArray())

            val response = transport().send(TransportRequest(HttpMethod.GET, plain.uri("/from")))

            response.text shouldBe "arrived"
        }

        test("stops at a chain that is too long") {
            (0..5).forEach { hop -> plain.redirect("/chain$hop", "/chain${hop + 1}") }
            plain.respond("/chain6")

            shouldThrow<NetworkException.Refused> {
                transport().send(TransportRequest(HttpMethod.GET, plain.uri("/chain0")))
            }

            val requests = plain.requests.filter { it.path == "/chain6" }
            requests.shouldBeEmpty()
        }

        test("does not follow a POST") {
            plain.redirect("/post", "/elsewhere", 307)
            plain.respond("/elsewhere")

            val post = TransportRequest(HttpMethod.POST, plain.uri("/post"), body = ByteArray(1))

            transport().send(post).statusCode shouldBe 307
            val requests = plain.requests.filter { it.path == "/elsewhere" }
            requests.shouldBeEmpty()
        }

        test("refuses the step from HTTPS down to HTTP") {
            secure.redirect("/downgrade", plain.uri("/stolen").toString())
            plain.respond("/stolen")
            val transport = transport(settings(caCertificates = LoopbackCertificate.pemFile))

            shouldThrow<NetworkException.Refused> {
                transport.send(TransportRequest(HttpMethod.GET, secure.uri("/downgrade")))
            }

            val requests = plain.requests.filter { it.path == "/stolen" }
            requests.shouldBeEmpty()
        }

        test("refuses a hop to a host outside the allowed hosts") {
            val other = LoopbackServer.http()
            other.use {
                plain.redirect("/foreign", other.uri("/stolen").toString())
                val request = TransportRequest(
                    HttpMethod.GET,
                    URI("http://localhost:${plain.uri("/").port}/foreign"),
                    allowedHosts = setOf("localhost")
                )

                shouldThrow<NetworkException.Refused> { transport().send(request) }

                other.requests.shouldBeEmpty()
            }
        }

        test("strips credentials on a cross-origin hop") {
            val other = LoopbackServer.http()
            other.use {
                other.respond("/landing")
                plain.redirect("/cross", other.uri("/landing").toString())
                val headers = mapOf("Authorization" to "Bearer token")

                transport().send(TransportRequest(HttpMethod.GET, plain.uri("/cross"), headers))

                plain.requests.last { it.path == "/cross" }.header("Authorization") shouldBe "Bearer token"
                val forwarded = other.requests.single()
                forwarded.header("Authorization").shouldBeNull()
            }
        }
    }

    context("retries") {
        test("retries a transient status with the fake sleeper") {
            plain.respondInTurn("/flaky", 503, 502, 200)
            val sleeper = RecordingSleeper()

            val response = transport(sleeper = sleeper).send(TransportRequest(HttpMethod.GET, plain.uri("/flaky")))

            response.statusCode shouldBe 200
            sleeper.delays shouldHaveSize 2
            sleeper.delays[0].toMillis() shouldBeInRange 500L..1000L
            sleeper.delays[1].toMillis() shouldBeInRange 1000L..2000L
        }

        test("honours Retry-After from a real response") {
            val statuses = ArrayDeque(listOf(429, 200))
            plain.handle("/limited") { exchange ->
                exchange.responseHeaders.add("Retry-After", "3")
                exchange.sendResponseHeaders(statuses.removeFirst(), -1)
            }
            val sleeper = RecordingSleeper()

            transport(sleeper = sleeper).send(TransportRequest(HttpMethod.GET, plain.uri("/limited")))

            sleeper.delays shouldBe listOf(Duration.ofSeconds(3))
        }

        test("does not retry an oversized body") {
            plain.respond("/huge", body = ByteArray(32))

            val request = TransportRequest(HttpMethod.GET, plain.uri("/huge"), maxBytes = 16)

            shouldThrow<NetworkException.TooLarge> { transport().send(request) }

            val requests = plain.requests.filter { it.path == "/huge" }
            requests shouldHaveSize 1
        }
    }

    context("TLS") {
        test("trusts an extra certificate authority from a PEM file") {
            secure.respond("/secure", body = "tls".toByteArray())
            val transport = transport(settings(caCertificates = LoopbackCertificate.pemFile))

            val response = transport.send(TransportRequest(HttpMethod.GET, secure.uri("/secure")))

            response.text shouldBe "tls"
        }

        test("does not trust the loopback certificate by default") {
            secure.respond("/untrusted")

            val failure = shouldThrow<NetworkException.Unavailable> {
                transport().send(TransportRequest(HttpMethod.GET, secure.uri("/untrusted")))
            }

            failure.message.orEmpty() shouldContain "after 3 attempt(s)"
        }

        test("refuses a transport with an unusable certificate file") {
            val missing = LoopbackCertificate.pemFile.resolveSibling("missing.pem")

            shouldThrow<NetworkException.Misconfigured> { transport(settings(caCertificates = missing)) }
        }
    }

    context("proxies") {
        test("keeps loopback traffic direct despite a dead proxy") {
            plain.respond("/direct", body = "direct".toByteArray())
            val settings = settings(proxy = ProxySettings("http://127.0.0.1:1"))

            val response = transport(settings).send(TransportRequest(HttpMethod.GET, plain.uri("/direct")))

            response.text shouldBe "direct"
        }

        test("refuses a transport with an unusable proxy") {
            shouldThrow<NetworkException.Misconfigured> {
                transport(settings(proxy = ProxySettings("socks5://proxy.test:1080")))
            }
        }
    }

    test("uses a working system sleeper by default") {
        plain.respond("/defaults")

        val transport = HttpTransports.create(settings(), FIXED_CLOCK)

        transport.send(TransportRequest(HttpMethod.GET, plain.uri("/defaults"))).statusCode shouldBe 200
    }
})
