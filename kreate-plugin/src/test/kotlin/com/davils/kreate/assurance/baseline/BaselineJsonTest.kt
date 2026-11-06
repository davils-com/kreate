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
package com.davils.kreate.assurance.baseline

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.AssuranceInputException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BaselineJsonTest : FunSpec({
    context("the baseline file") {
        test("survives a round trip") {
            val entry = BaselineEntry("osv", "GHSA-1", 2, Severity.HIGH, "Title", "com.example:lib", "a.kt")

            BaselineJson.parse(BaselineJson.render(listOf(entry)), "baseline") shouldBe listOf(entry)
        }

        test("renders the same entries the same way, whatever their order") {
            val first = BaselineEntry("osv", "A", 1, Severity.LOW, "t")
            val second = BaselineEntry("osv", "B", 1, Severity.LOW, "t")

            BaselineJson.render(listOf(second, first)) shouldBe BaselineJson.render(listOf(first, second))
        }

        mapOf(
            "not JSON" to "{",
            "no entries" to "{}",
            "an unknown field" to
                """{"entries":[{"source":"o","rule":"r","count":1,"severity":"LOW","title":"t","x":"y"}]}""",
            "a count of zero" to """{"entries":[{"source":"o","rule":"r","count":0,"severity":"LOW","title":"t"}]}""",
            "an unknown severity" to """{"entries":[{"source":"o","rule":"r","count":1,"severity":"X","title":"t"}]}"""
        ).forEach { (name, text) ->
            test("refuses $name") {
                val failure = shouldThrow<AssuranceInputException> { BaselineJson.parse(text, "kreate-baseline.json") }

                failure.message.orEmpty() shouldContain "kreate-baseline.json"
            }
        }
    }
})
