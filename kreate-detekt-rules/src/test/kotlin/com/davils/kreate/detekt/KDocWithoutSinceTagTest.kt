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
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the rule that requires `@since` on documented public declarations.
 *
 * Two boundaries are covered deliberately: a non-public declaration, which another rule owns, and a
 * single-line comment, where the KDoc parser does not recognise the tag but a reader does.
 */
@DisplayName("KDocWithoutSinceTag")
class KDocWithoutSinceTagTest {

    @Nested
    @DisplayName("reports")
    inner class Reports {

        @Test
        @DisplayName("a public function documented without a version")
        fun functionWithoutSince() {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    /**
                     * Exchanges keys.
                     *
                     * @return The shared secret.
                     */
                    public fun exchange(): String = ""
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a public property documented without a version")
        fun propertyWithoutSince() {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    public class Exchanger {
                        /**
                         * The identifier.
                         */
                        public val id: String = ""
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }
    }

    @Nested
    @DisplayName("leaves alone")
    inner class LeavesAlone {

        @Test
        @DisplayName("a block that carries the tag")
        fun withSince() {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    /**
                     * Exchanges keys.
                     *
                     * @since 1.0.0
                     */
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a single-line block that carries the tag, which the KDoc parser cannot see")
        fun singleLineWithSince() {
            // The tag is only a tag to the KDoc lexer at the start of a line. A rule reading the
            // PSI would report this block as missing the version it plainly states.
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    /** Exchanges keys. @since 1.0.0 */
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a non-public declaration, which KDocOnNonPublicDeclaration owns")
        fun nonPublicDeclaration() {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    /**
                     * Exchanges keys.
                     */
                    internal fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("an undocumented declaration, which the comments rule set owns")
        fun undocumentedDeclaration() {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
}
