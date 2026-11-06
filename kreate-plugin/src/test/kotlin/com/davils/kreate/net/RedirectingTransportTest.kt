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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.URI

private fun redirectTo(location: String, status: Int = 302): (TransportRequest) -> TransportResponse =
    { responseOf(status, it.uri.toString(), mapOf("Location" to location)) }

private fun success(): (TransportRequest) -> TransportResponse = { responseOf(200, it.uri.toString()) }

class RedirectingTransportTest : FunSpec({
    test("follows every redirect status") {
        listOf(301, 302, 303, 307, 308).forEach { status ->
            val delegate = ScriptedTransport.inTurn(redirectTo("/next", status), success())

            val response = RedirectingTransport(delegate).send(get("https://example.com/first"))

            response.uri shouldBe URI("https://example.com/next")
        }
    }

    test("does not treat other 3xx responses as redirects") {
        listOf(300, 304, 305, 306).forEach { status ->
            val delegate = ScriptedTransport.inTurn(redirectTo("/next", status))

            val response = RedirectingTransport(delegate).send(get("https://example.com/"))

            response.statusCode shouldBe status
        }
    }

    test("returns a redirect without a location as it is") {
        val delegate = ScriptedTransport { responseOf(302) }

        val response = RedirectingTransport(delegate).send(get("https://example.com/"))

        response.statusCode shouldBe 302
    }

    test("follows five redirects") {
        val delegate = ScriptedTransport.inTurn(
            redirectTo("/1"), redirectTo("/2"), redirectTo("/3"), redirectTo("/4"), redirectTo("/5"), success()
        )

        val response = RedirectingTransport(delegate).send(get("https://example.com/"))

        response.statusCode shouldBe 200
        delegate.requests shouldHaveSize 6
    }

    test("refuses a sixth redirect before it is requested") {
        val delegate = ScriptedTransport(redirectTo("/x"))

        val failure = shouldThrow<NetworkException.Refused> {
            RedirectingTransport(delegate).send(get("https://example.com/"))
        }

        failure.message.orEmpty() shouldContain "more than 5"
        delegate.requests shouldHaveSize 6
    }

    test("refuses a loop of redirects") {
        val delegate = ScriptedTransport { request ->
            val isOnFirstPage = request.uri.path == "/a"
            val next = "/b".takeIf { isOnFirstPage } ?: "/a"
            responseOf(302, request.uri.toString(), mapOf("Location" to next))
        }

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(get("https://example.com/a")) }
    }

    test("never redirects a POST") {
        val delegate = ScriptedTransport.inTurn(redirectTo("/next", 307))
        val post = TransportRequest(HttpMethod.POST, URI("https://example.com/"), body = ByteArray(1))

        val response = RedirectingTransport(delegate).send(post)

        response.statusCode shouldBe 307
        delegate.requests shouldHaveSize 1
    }

    test("keeps the method of a HEAD request") {
        val delegate = ScriptedTransport.inTurn(redirectTo("/next"), success())
        val head = TransportRequest(HttpMethod.HEAD, URI("https://example.com/"))

        RedirectingTransport(delegate).send(head)

        delegate.requests.last().method shouldBe HttpMethod.HEAD
    }

    test("refuses the first request before the delegate sees it") {
        val delegate = ScriptedTransport { error("The delegate must not be called.") }

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(get("http://example.com/")) }
    }

    test("revalidates every hop, not only the first") {
        val delegate = ScriptedTransport.inTurn(
            redirectTo("https://cdn.example.org/a"),
            redirectTo("/b"),
            redirectTo("http://cdn.example.org/c")
        )

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(get("https://example.com/")) }

        delegate.requests shouldHaveSize 3
    }

    test("refuses to enter the local network from a later hop") {
        val delegate = ScriptedTransport.inTurn(
            redirectTo("https://cdn.example.org/a"),
            redirectTo("https://169.254.169.254/latest/meta-data/")
        )

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(get("https://example.com/")) }
    }

    test("resolves scheme-relative locations before checking them") {
        val delegate = ScriptedTransport.inTurn(redirectTo("//127.0.0.1/x"))

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(get("https://example.com/")) }
    }

    test("refuses an unparseable location") {
        val delegate = ScriptedTransport.inTurn(redirectTo("https://exa mple.com/"))

        val failure = shouldThrow<NetworkException.Refused> {
            RedirectingTransport(delegate).send(get("https://example.com/"))
        }

        failure.message.orEmpty() shouldContain "invalid target"
    }

    test("enforces allowed hosts on every hop") {
        val delegate = ScriptedTransport.inTurn(redirectTo("https://example.org/"))
        val request = TransportRequest(HttpMethod.GET, URI("https://example.com/"), allowedHosts = setOf("example.com"))

        shouldThrow<NetworkException.Refused> { RedirectingTransport(delegate).send(request) }
    }

    context("request headers") {
        val headers = mapOf("Authorization" to "Bearer token", "Accept" to "application/json", "X-Api-Key" to "k")

        test("keeps them for the same origin") {
            val delegate = ScriptedTransport.inTurn(redirectTo("/next"), success())

            RedirectingTransport(delegate).send(get("https://example.com/", headers))

            delegate.requests.last().headers shouldBe headers
        }

        test("treats an explicit default port as the same origin") {
            val delegate = ScriptedTransport.inTurn(redirectTo("https://example.com:443/next"), success())

            RedirectingTransport(delegate).send(get("https://example.com/", headers))

            delegate.requests.last().headers shouldBe headers
        }

        test("treats the default HTTP port as the same origin") {
            val delegate = ScriptedTransport.inTurn(redirectTo("http://localhost:80/next"), success())

            RedirectingTransport(delegate).send(get("http://localhost/", headers))

            delegate.requests.last().headers shouldBe headers
        }

        listOf(
            "https://cdn.example.org/next",
            "https://example.com:8443/next"
        ).forEach { target ->
            test("drops credentials on the way to $target") {
                val delegate = ScriptedTransport.inTurn(redirectTo(target), success())

                RedirectingTransport(delegate).send(get("https://example.com/", headers))

                delegate.requests.last().headers shouldBe mapOf("Accept" to "application/json")
            }
        }

        test("drops credentials when the scheme changes") {
            val delegate = ScriptedTransport.inTurn(redirectTo("https://localhost/next"), success())

            RedirectingTransport(delegate).send(get("http://localhost/", headers))

            delegate.requests.last().headers["Authorization"].shouldBeNull()
        }
    }
})
