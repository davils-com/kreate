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
import io.kotest.matchers.shouldBe
import java.net.URI

class TransportRequestTest : FunSpec({
    test("defaults to the Inspectra body limit") {
        get("https://example.com/").maxBytes shouldBe 64 * 1024 * 1024
    }

    test("refuses a limit that leaves no room to detect an overrun") {
        listOf(-1, Int.MAX_VALUE).forEach { limit ->
            shouldThrow<IllegalArgumentException> {
                TransportRequest(HttpMethod.GET, URI("https://example.com/"), maxBytes = limit)
            }
        }
    }

    test("carries everything but the address and headers to the next hop") {
        val body = ByteArray(1)
        val request = TransportRequest(
            HttpMethod.POST,
            URI("https://example.com/"),
            mapOf("a" to "b"),
            body,
            maxBytes = 5,
            allowedHosts = setOf("example.org")
        )

        val next = request.redirectedTo(URI("https://example.org/"), emptyMap())

        next.uri shouldBe URI("https://example.org/")
        next.headers shouldBe emptyMap()
        next.method shouldBe HttpMethod.POST
        next.body shouldBe body
        next.maxBytes shouldBe 5
        next.allowedHosts shouldBe setOf("example.org")
    }
})
