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

package com.davils.kreate.rules.controlflow

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ForbiddenElseTest : FunSpec({

    context("reports") {
        test("an else block after an if") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun sign(value: Int): String {
                        if (value < 0) {
                            return "negative"
                        } else {
                            return "positive"
                        }
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("an if expression with an else value") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun sign(value: Int): String = if (value < 0) "negative" else "positive"
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("every else in an else-if ladder") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun sign(value: Int): String {
                        if (value < 0) {
                            return "negative"
                        } else if (value == 0) {
                            return "zero"
                        } else {
                            return "positive"
                        }
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 2
        }

        test("an else branch of a when") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun describe(code: Int): String = when (code) {
                        200 -> "ok"
                        else -> "failure"
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("at the else keyword") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun sign(value: Int): String {
                        if (value < 0) {
                            return "negative"
                        }
                        else {
                            return "positive"
                        }
                    }
                """.trimIndent()
            )

            val location = findings.single().entity.location
            location.source.line shouldBe 5
        }
    }

    context("leaves alone") {
        test("an if with an early return") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public fun sign(value: Int): String {
                        if (value < 0) return "negative"
                        return "positive"
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("an exhaustive when over an enum") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public enum class Light { RED, GREEN }

                    public fun describe(light: Light): String = when (light) {
                        Light.RED -> "stop"
                        Light.GREEN -> "go"
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("the word else inside a string") {
            val findings = ForbiddenElse(Config.empty).lint(
                """
                    public val text: String = "if this, else that"
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
