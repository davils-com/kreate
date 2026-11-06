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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URI

class NoProxyRulesTest : FunSpec({
    listOf(
        Triple("example.com", "https://example.com/", true),
        Triple("example.com", "https://EXAMPLE.com/", true),
        Triple("EXAMPLE.com", "https://example.com/", true),
        Triple("example.com", "https://a.example.com/", true),
        Triple(".example.com", "https://a.example.com/", true),
        Triple(".example.com", "https://example.com/", true),
        Triple("example.com", "https://notexample.com/", false),
        Triple("example.com", "https://example.com.evil.test/", false),
        Triple("a.test, example.com", "https://example.com/", true),
        Triple(" , ,", "https://example.com/", false),
        Triple("*", "https://example.com/", true),
        Triple("a.test,*", "https://example.com/", true),
        Triple("example.com:8443", "https://example.com:8443/", true),
        Triple("example.com:8443", "https://example.com/", false),
        Triple("example.com:443", "https://example.com/", true),
        Triple("10.0.0.1", "https://10.0.0.1/", true),
        Triple("10.0.0.1", "https://10.0.0.2/", false),
        Triple("[fd00::1]", "https://[fd00::1]/", true),
        Triple("[fd00::1]:8443", "https://[fd00::1]:8443/", true),
        Triple("fd00::1:2", "https://[fd00::1:2]/", true),
        Triple("example.com", "/relative", false)
    ).forEach { (rules, uri, expected) ->
        test("'$rules' against $uri is $expected") {
            NoProxyRules(rules).matches(URI(uri)) shouldBe expected
        }
    }

    test("matches nothing without a value") {
        NoProxyRules(null).matches(URI("https://example.com/")) shouldBe false
    }
})
