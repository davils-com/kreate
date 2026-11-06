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

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.net.URI

class UriGuardTest : FunSpec({
    val guard = UriGuard(emptySet())

    context("a request") {
        listOf(
            "https://example.com/x",
            "https://10.0.0.1/mirror",
            "http://127.0.0.1:8080/x",
            "http://localhost/x",
            "http://[::1]:8080/x",
            "HTTPS://EXAMPLE.com/x"
        ).forEach { uri ->
            test("accepts $uri") {
                shouldNotThrowAny { guard.check(URI(uri)) }
            }
        }

        listOf(
            "http://example.com/x",
            "http://10.0.0.1/x",
            "http://0.0.0.0/x",
            "ftp://example.com/x",
            "file:///etc/passwd",
            "jar:https://example.com/x.jar!/a",
            "https://user:secret@example.com/x",
            "https://example.com@evil.test/x",
            "https://2130706433/x",
            "http://2130706433/x",
            "http://0x7f.1/x",
            "https://under_score/x",
            "/relative"
        ).forEach { uri ->
            test("refuses $uri") {
                shouldThrow<NetworkException.Refused> { guard.check(URI(uri)) }
            }
        }

        test("does not repeat embedded credentials in the message") {
            val failure = shouldThrow<NetworkException.Refused> { guard.check(URI("https://user:secret@example.com/")) }

            failure.message.orEmpty() shouldNotContain "secret"
        }
    }

    context("allowed hosts") {
        val restricted = UriGuard(setOf("Example.com"))

        test("accepts a listed host in any case") {
            shouldNotThrowAny { restricted.check(URI("https://EXAMPLE.COM/x")) }
        }

        test("refuses a subdomain, which is not the listed host") {
            shouldThrow<NetworkException.Refused> { restricted.check(URI("https://a.example.com/x")) }
        }

        test("refuses a host that merely ends like a listed one") {
            shouldThrow<NetworkException.Refused> { restricted.check(URI("https://evilexample.com/x")) }
        }
    }

    context("a redirect hop") {
        test("may move between public hosts") {
            shouldNotThrowAny { guard.checkHop(URI("https://example.com/"), URI("https://cdn.example.org/")) }
        }

        test("may stay on the loopback interface") {
            shouldNotThrowAny { guard.checkHop(URI("http://127.0.0.1:1/"), URI("http://localhost:2/")) }
        }

        test("may stay inside a private network") {
            shouldNotThrowAny { guard.checkHop(URI("https://10.0.0.1/"), URI("https://10.0.0.2/")) }
        }

        test("may not leave HTTPS") {
            val failure = shouldThrow<NetworkException.Refused> {
                guard.checkHop(URI("https://localhost/"), URI("http://localhost/"))
            }

            failure.message.orEmpty() shouldContain "leave HTTPS"
        }

        test("may not reach plain HTTP on a public host") {
            shouldThrow<NetworkException.Refused> {
                guard.checkHop(URI("https://example.com/"), URI("http://example.com/"))
            }
        }

        listOf(
            "https://127.0.0.1/",
            "https://localhost/",
            "https://[::1]/",
            "https://169.254.169.254/latest/meta-data/",
            "https://10.0.0.1/",
            "https://192.168.0.1/",
            "https://[fd00::1]/"
        ).forEach { target ->
            test("may not move from a public host to $target") {
                shouldThrow<NetworkException.Refused> { guard.checkHop(URI("https://example.com/"), URI(target)) }
            }
        }

        test("refuses a target with credentials") {
            shouldThrow<NetworkException.Refused> {
                guard.checkHop(URI("https://example.com/"), URI("https://a:b@example.org/"))
            }
        }

        test("refuses a target outside the allowed hosts") {
            val restricted = UriGuard(setOf("example.com"))

            shouldThrow<NetworkException.Refused> {
                restricted.checkHop(URI("https://example.com/"), URI("https://example.org/"))
            }
        }

        test("refuses an origin that never had a host") {
            shouldThrow<NetworkException.Refused> { guard.checkHop(URI("/relative"), URI("https://example.org/")) }
        }
    }
})
