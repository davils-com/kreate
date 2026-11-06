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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class ProxyAddressTest : FunSpec({
    test("reads an HTTP proxy address") {
        val address = ProxyAddress.parse("http://proxy.example.com:3128")

        address.hostString shouldBe "proxy.example.com"
        address.port shouldBe 3128
    }

    test("does not resolve the proxy host") {
        ProxyAddress.parse("http://proxy.example.com:3128").isUnresolved.shouldBeTrue()
    }

    test("treats a value without a scheme as HTTP") {
        ProxyAddress.parse(" proxy.example.com:8080 ").port shouldBe 8080
    }

    test("defaults to port 80") {
        ProxyAddress.parse("http://proxy.example.com").port shouldBe 80
    }

    listOf(
        "https://proxy.example.com:3128",
        "socks5://proxy.example.com:1080",
        "http://",
        "http://bad host:1"
    ).forEach { value ->
        test("refuses $value") {
            shouldThrow<NetworkException.Misconfigured> { ProxyAddress.parse(value) }
        }
    }

    test("refuses credentials without repeating them") {
        val failure = shouldThrow<NetworkException.Misconfigured> {
            ProxyAddress.parse("http://user:secret@proxy.example.com:3128")
        }

        failure.message.orEmpty() shouldNotContain "secret"
    }
})
