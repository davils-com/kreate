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
package com.davils.kreate.standard.fetch

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.freshDirectory
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.HttpTransports
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.LoopbackServer.Companion.reply
import com.davils.kreate.net.NetworkSettings
import com.davils.kreate.net.RetryPolicy
import com.davils.kreate.net.Sleeper
import com.davils.kreate.standard.model.LookupFailure
import com.sun.net.httpserver.HttpExchange
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Duration

private const val HTTP_OK: Int = 200

private const val HTTP_NOT_MODIFIED: Int = 304

private const val HTTP_FAILURE: Int = 500

private val DAY: Duration = Duration.ofHours(24)

private fun transport(): HttpTransport =
    HttpTransports.create(NetworkSettings("test"), SteppingClock(), Sleeper { }, RetryPolicy(maxAttempts = 1))

private fun answer(exchange: HttpExchange, failing: Boolean) {
    val headers = exchange.requestHeaders
    val validator = headers.getFirst("If-None-Match")
    if (failing) return exchange.reply(HTTP_FAILURE)
    if (validator == "\"v1\"") return exchange.reply(HTTP_NOT_MODIFIED)
    exchange.reply(HTTP_OK, mapOf("ETag" to "\"v1\""), "the text".toByteArray())
}

private fun Outcome<FetchedDocument, LookupFailure>.text(): String {
    val document = document()
    return document.text()
}

private fun Outcome<FetchedDocument, LookupFailure>.document(): FetchedDocument {
    val success = shouldBeInstanceOf<Outcome.Success<FetchedDocument>>()
    return success.value
}

class RevalidatingCacheTest : FunSpec({
    lateinit var server: LoopbackServer
    lateinit var cache: RevalidatingCache
    lateinit var clock: SteppingClock
    var failing = false

    beforeEach {
        server = LoopbackServer.http()
        clock = SteppingClock()
        val directory = tempdir().freshDirectory()
        cache = RevalidatingCache(directory.toPath(), DAY, clock)
        failing = false
        server.handle("/spec") { exchange -> answer(exchange, failing) }
    }

    afterEach { server.close() }

    test("answers from the cache while the copy is fresh") {
        transport().use { transport ->
            cache.fetch(FetchRequest(server.uri("/spec")), transport).text() shouldBe "the text"
            clock.advance(Duration.ofHours(23))
            cache.fetch(FetchRequest(server.uri("/spec")), transport).text() shouldBe "the text"
        }

        server.requests.size shouldBe 1
    }

    test("revalidates a stale copy with its validator and keeps it when nothing changed") {
        transport().use { transport ->
            cache.fetch(FetchRequest(server.uri("/spec")), transport)
            clock.advance(DAY.plusMinutes(1))

            val document = cache.fetch(FetchRequest(server.uri("/spec")), transport).document()

            document.text() shouldBe "the text"
            document.fetchedAt shouldBe clock.instant()
        }

        server.requests.last().header("If-None-Match") shouldBe "\"v1\""
    }

    test("keeps a stale copy with its age when the service fails") {
        transport().use { transport ->
            val first = cache.fetch(FetchRequest(server.uri("/spec")), transport).document()
            clock.advance(DAY.multipliedBy(2))
            failing = true

            val stale = cache.fetch(FetchRequest(server.uri("/spec")), transport).document()

            stale.fetchedAt shouldBe first.fetchedAt
            cache.oldestAnswer shouldBe first.fetchedAt
        }
    }

    test("is unavailable without a copy when the service fails") {
        failing = true

        transport().use { transport ->
            val outcome = cache.fetch(FetchRequest(server.uri("/spec")), transport)

            outcome.shouldBeInstanceOf<Outcome.Failure<LookupFailure.Unavailable>>()
        }
    }

    test("does not know what the service does not have") {
        transport().use { transport ->
            val outcome = cache.fetch(FetchRequest(server.uri("/missing")), transport)

            val failure = outcome.shouldBeInstanceOf<Outcome.Failure<LookupFailure>>()
            failure.failure.shouldBeInstanceOf<LookupFailure.Unknown>()
        }
    }

    test("refuses a redirect to another host") {
        server.redirect("/moved", "http://localhost:1/spec")

        transport().use { transport ->
            val outcome = cache.fetch(FetchRequest(server.uri("/moved")), transport)

            val failure = outcome.shouldBeInstanceOf<Outcome.Failure<LookupFailure>>()
            failure.failure.shouldBeInstanceOf<LookupFailure.Unavailable>()
        }
    }
})
