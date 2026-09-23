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

package com.davils.kreate.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the rule that forbids `//` comments.
 *
 * The cases that matter are the ones a text based search gets wrong: a `//` inside a string
 * literal, a URL in a copyright header, and a KDoc block that happens to mention one.
 */
@DisplayName("ForbiddenLineComment")
class ForbiddenLineCommentTest {

    @Nested
    @DisplayName("reports")
    inner class Reports {

        @Test
        @DisplayName("a comment on a line of its own")
        fun standaloneComment() {
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

        @Test
        @DisplayName("a comment trailing code")
        fun trailingComment() {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public val timeout: Int = 30 // seconds
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("each comment separately, so a fix can be tracked line by line")
        fun oneFindingPerComment() {
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
    }

    @Nested
    @DisplayName("leaves alone")
    inner class LeavesAlone {

        @Test
        @DisplayName("a block comment, which is how a copyright header is written")
        fun blockComment() {
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

        @Test
        @DisplayName("a KDoc block")
        fun documentation() {
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

        @Test
        @DisplayName("a double slash inside a string literal")
        fun slashesInString() {
            val findings = ForbiddenLineComment(Config.empty).lint(
                """
                    public val endpoint: String = "https://example.com/keys"
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }

    @Nested
    @DisplayName("allowedPattern")
    inner class AllowedPattern {

        @Test
        @DisplayName("exempts a comment the pattern matches")
        fun exemptsMatch() {
            val rule = ForbiddenLineComment(TestConfig("allowedPattern" to "^region\\b"))

            val findings = rule.lint(
                """
                    // region Serialization
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("still reports a comment the pattern does not match")
        fun reportsNonMatch() {
            val rule = ForbiddenLineComment(TestConfig("allowedPattern" to "^region\\b"))

            val findings = rule.lint(
                """
                    // Retry once.
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("allows nothing when empty, rather than everything")
        fun emptyPatternAllowsNothing() {
            // An empty regular expression matches every string. Read naively, the default value
            // would switch the rule off for every project that never configures it.
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

    @Test
    @DisplayName("points at the line the comment is on")
    fun reportsAtTheComment() {
        val findings = ForbiddenLineComment(Config.empty).lint(
            """
                public fun run() {
                    // Retry once.
                    attempt()
                }
            """.trimIndent()
        )

        findings.single().entity.location.source.line shouldBe 2
    }
}
