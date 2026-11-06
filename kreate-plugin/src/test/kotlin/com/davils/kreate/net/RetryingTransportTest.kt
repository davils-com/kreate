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
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.URI
import java.time.Duration
import kotlin.random.Random

private fun status(code: Int, retryAfter: String? = null): (TransportRequest) -> TransportResponse {
    val headers = retryAfter?.let { mapOf("Retry-After" to it) }.orEmpty()
    return { responseOf(code, headers = headers) }
}

private fun unavailable(): (TransportRequest) -> TransportResponse =
    { throw NetworkException.Unavailable("connection reset") }

class RetryingTransportTest : FunSpec({
    val policy = RetryPolicy(baseDelay = Duration.ofSeconds(1), random = Random(7))

    fun transport(delegate: HttpTransport, sleeper: Sleeper): RetryingTransport =
        RetryingTransport(delegate, policy, sleeper)

    test("does not wait when the first attempt succeeds") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport.inTurn(status(200))

        val response = transport(delegate, sleeper).send(get("https://example.com/"))

        response.statusCode shouldBe 200

        sleeper.delays.shouldBeEmpty()
    }

    test("does not retry a status that is not transient") {
        val delegate = ScriptedTransport.inTurn(status(404))

        val response = transport(delegate, RecordingSleeper()).send(get("https://example.com/"))

        response.isNotFound.shouldBeTrue()
        delegate.requests shouldHaveSize 1
    }

    test("backs off exponentially between attempts") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport.inTurn(status(503), status(503), status(200))

        val response = transport(delegate, sleeper).send(get("https://example.com/"))

        response.statusCode shouldBe 200

        sleeper.delays shouldHaveSize 2
        sleeper.delays[0].toMillis() shouldBeInRange 500L..1000L
        sleeper.delays[1].toMillis() shouldBeInRange 1000L..2000L
    }

    test("returns the last response when the attempts run out") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport { responseOf(429) }

        val response = transport(delegate, sleeper).send(get("https://example.com/"))

        response.statusCode shouldBe 429

        delegate.requests shouldHaveSize 3
        sleeper.delays shouldHaveSize 2
    }

    test("lets Retry-After decide the delay") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport.inTurn(status(429, "7"), status(200))

        transport(delegate, sleeper).send(get("https://example.com/"))

        sleeper.delays shouldBe listOf(Duration.ofSeconds(7))
    }

    test("caps a hostile Retry-After") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport.inTurn(status(503, "86400"), status(200))

        transport(delegate, sleeper).send(get("https://example.com/"))

        sleeper.delays shouldBe listOf(Duration.ofSeconds(30))
    }

    test("ignores a Retry-After it cannot read") {
        listOf("Wed, 21 Oct 2026 07:28:00 GMT", "-5", "", "soon").forEach { value ->
            val sleeper = RecordingSleeper()
            val delegate = ScriptedTransport.inTurn(status(503, value), status(200))

            transport(delegate, sleeper).send(get("https://example.com/"))

            sleeper.delays.single().toMillis() shouldBeInRange 500L..1000L
        }
    }

    test("retries a connection failure and backs off") {
        val sleeper = RecordingSleeper()
        val delegate = ScriptedTransport.inTurn(unavailable(), status(200))

        val response = transport(delegate, sleeper).send(get("https://example.com/"))

        response.statusCode shouldBe 200

        sleeper.delays shouldHaveSize 1
    }

    test("reports the attempts when every one failed") {
        val delegate = ScriptedTransport { throw NetworkException.Unavailable("connection reset") }

        val failure = shouldThrow<NetworkException.Unavailable> {
            transport(delegate, RecordingSleeper()).send(get("https://example.com/"))
        }

        failure.message.orEmpty() shouldContain "after 3 attempt(s)"
        failure.message.orEmpty() shouldContain "connection reset"
        delegate.requests shouldHaveSize 3
    }

    test("retries a POST, because the queries it carries are idempotent") {
        val delegate = ScriptedTransport.inTurn(status(503), status(200))
        val post = TransportRequest(HttpMethod.POST, URI("https://example.com/"), body = ByteArray(1))

        val response = transport(delegate, RecordingSleeper()).send(post)

        response.statusCode shouldBe 200

        delegate.requests shouldHaveSize 2
    }

    listOf(
        NetworkException.Refused("no"),
        NetworkException.TooLarge("no"),
        NetworkException.Interrupted("no"),
        NetworkException.Offline("no"),
        NetworkException.Misconfigured("no")
    ).forEach { refusal ->
        test("does not retry ${refusal::class.simpleName}") {
            val delegate = ScriptedTransport { throw refusal }

            val failure = shouldThrow<NetworkException> {
                transport(delegate, RecordingSleeper()).send(get("https://example.com/"))
            }

            failure shouldBe refusal
            delegate.requests shouldHaveSize 1
        }
    }

    test("gives up and keeps the interrupt when the wait is interrupted") {
        val delegate = ScriptedTransport { responseOf(503) }
        val sleeper = Sleeper { throw InterruptedException("stop") }

        shouldThrow<NetworkException.Interrupted> {
            transport(delegate, sleeper).send(get("https://example.com/"))
        }

        Thread.interrupted().shouldBeTrue()
        delegate.requests shouldHaveSize 1
    }

    test("the system sleeper waits") {
        val start = System.nanoTime()

        Sleeper.SYSTEM.sleep(Duration.ofMillis(5))

        val elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis()
        elapsedMillis shouldBeInRange 5L..60_000L
    }
})
