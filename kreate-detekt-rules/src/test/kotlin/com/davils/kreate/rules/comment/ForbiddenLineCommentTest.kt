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

package com.davils.kreate.rules.comment

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ForbiddenLineCommentTest : FunSpec({

    context("reports") {
        test("a comment on a line of its own") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public fun run() {
                        // Retry once, because the first attempt warms the connection pool.
                        attempt()
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a comment trailing code") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public val timeout: Int = 30 // seconds
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("each comment separately, so a fix can be tracked line by line") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    // First.
                    public fun run() {
                        // Second.
                        attempt() // Third.
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 3
        }

        test("at the line the comment is on") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public fun run() {
                        // Retry once.
                        attempt()
                    }
                """.trimIndent()
            )

            val location = findings.single().entity.location
            location.source.line shouldBe 2
        }
    }

    context("leaves alone") {
        test("a block comment, which ForbiddenBlockComment owns") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    /*
                     * Copyright 2026 Davils
                     *
                     *     http://www.apache.org/licenses/LICENSE-2.0
                     */
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a KDoc block") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    /**
                     * Runs the attempt. See https://example.com/retries for the reasoning.
                     *
                     * @since 1.0.0
                     */
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a double slash inside a string literal") {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public val endpoint: String = "https://example.com/keys"
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }

    context("allowedPattern") {
        test("exempts a comment the pattern matches") {
            val rule = ForbiddenLineComment(TestConfig("allowedPattern" to "^region\\b"))

            val findings = rule.lint(
                """
                    // region Serialization
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("still reports a comment the pattern does not match") {
            val rule = ForbiddenLineComment(TestConfig("allowedPattern" to "^region\\b"))

            val findings = rule.lint(
                """
                    // Retry once.
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("allows nothing when empty, although an empty expression matches every string") {
            val rule = ForbiddenLineComment(TestConfig("allowedPattern" to ""))

            val findings = rule.lint(
                """
                    // Retry once.
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }
    }
})
