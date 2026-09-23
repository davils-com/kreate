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
 * Tests for the rule that keeps a KDoc block attached to its declaration.
 *
 * The case worth stating outright is the annotated declaration: the annotation belongs to the
 * declaration, so the blank line is still between the comment and the thing it documents.
 */
@DisplayName("KDocSeparatedFromDeclaration")
class KDocSeparatedFromDeclarationTest {

    @Nested
    @DisplayName("reports")
    inner class Reports {

        @Test
        @DisplayName("a blank line before a class")
        fun blankLineBeforeClass() {
            val findings = KDocSeparatedFromDeclaration(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     *
                     * @since 1.0.0
                     */

                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a blank line before an annotated declaration")
        fun blankLineBeforeAnnotated() {
            val findings = KDocSeparatedFromDeclaration(Config.empty).lint(
                """
                    /**
                     * Exchanges keys.
                     *
                     * @since 1.0.0
                     */

                    @JvmName("exchangeKeys")
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a blank line before a property inside a class body")
        fun blankLineBeforeProperty() {
            val findings = KDocSeparatedFromDeclaration(Config.empty).lint(
                """
                    public class Exchanger {
                        /**
                         * The identifier.
                         *
                         * @since 1.0.0
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
        @DisplayName("a block that sits directly above its declaration")
        fun attachedBlock() {
            val findings = KDocSeparatedFromDeclaration(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     *
                     * @since 1.0.0
                     */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a block that documents nothing below it")
        fun blockWithoutDeclaration() {
            val findings = KDocSeparatedFromDeclaration(Config.empty).lint(
                """
                    public class Exchanger

                    /**
                     * A note that documents no declaration at all.
                     */
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
}
