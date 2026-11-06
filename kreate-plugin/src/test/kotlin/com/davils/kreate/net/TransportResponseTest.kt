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
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.net.URI
import java.time.Duration

class TransportResponseTest : FunSpec({
    test("calls 2xx a success") {
        responseOf(199).isSuccess.shouldBeFalse()
        responseOf(200).isSuccess.shouldBeTrue()
        responseOf(299).isSuccess.shouldBeTrue()
        responseOf(300).isSuccess.shouldBeFalse()
    }

    test("recognises a missing resource") {
        responseOf(404).isNotFound.shouldBeTrue()
        responseOf(200).isNotFound.shouldBeFalse()
    }

    test("decodes the body as UTF-8") {
        responseOf(200, body = "\u00E9".toByteArray()).text shouldBe "\u00E9"
    }

    test("finds headers in any case") {
        val response = responseOf(200, headers = mapOf("X-Thing" to "1"))

        response.header("x-THING") shouldBe "1"
        response.header("absent").shouldBeNull()
    }

    test("reads Retry-After seconds") {
        val response = responseOf(429, headers = mapOf("Retry-After" to "12"))

        response.retryAfter shouldBe Duration.ofSeconds(12)
    }

    test("survives a Retry-After beyond what a duration in milliseconds holds") {
        val response = responseOf(429, headers = mapOf("Retry-After" to Long.MAX_VALUE.toString()))

        response.retryAfter shouldBe Duration.ofSeconds(Long.MAX_VALUE)
    }

    test("has no Retry-After without the header") {
        responseOf(429).retryAfter.shouldBeNull()
    }

    test("resolves a relative redirect against the response") {
        val response = responseOf(302, "https://example.com/a/b", mapOf("Location" to "../c"))

        response.redirectTarget() shouldBe URI("https://example.com/c")
    }

    test("has no redirect target without a location") {
        val response = responseOf(302)

        response.redirectTarget().shouldBeNull()
    }

    test("refuses a location that is not a URI") {
        val response = responseOf(302, headers = mapOf("Location" to "http://[bad"))

        shouldThrow<NetworkException.Refused> { response.redirectTarget() }
    }

    test("keeps the time the clock reported") {
        responseOf(200).receivedAt shouldBe FIXED_INSTANT
    }
})
