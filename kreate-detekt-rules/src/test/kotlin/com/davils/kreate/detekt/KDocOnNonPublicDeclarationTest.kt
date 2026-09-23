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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the rule that keeps KDoc on the published surface.
 *
 * The interesting cases are the ones where the declaration's own modifier does not decide: a
 * `public` member of an `internal` class, a class declared inside a function, and a property
 * declared in a constructor whose own visibility is narrower than the type's.
 */
@DisplayName("KDocOnNonPublicDeclaration")
class KDocOnNonPublicDeclarationTest {

    @Nested
    @DisplayName("reports")
    inner class Reports {

        @Test
        @DisplayName("a private function")
        fun privateFunction() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    /**
                     * Exchanges keys.
                     *
                     * @since 1.0.0
                     */
                    private fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("an internal class")
        fun internalClass() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     *
                     * @since 1.0.0
                     */
                    internal class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a public member of an internal class, which no consumer can reach")
        fun publicMemberOfInternalClass() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    internal class Exchanger {
                        /**
                         * Exchanges keys.
                         *
                         * @since 1.0.0
                         */
                        public fun exchange(): Unit = Unit
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a declaration inside a function body")
        fun localDeclaration() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    public fun run() {
                        /**
                         * A local helper.
                         *
                         * @since 1.0.0
                         */
                        class Helper

                        Helper()
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        @Test
        @DisplayName("a protected member, by default")
        fun protectedMember() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    public open class Exchanger {
                        /**
                         * Exchanges keys.
                         *
                         * @since 1.0.0
                         */
                        protected fun exchange(): Unit = Unit
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
        @DisplayName("a public declaration")
        fun publicDeclaration() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
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
        @DisplayName("a declaration without documentation")
        fun undocumentedDeclaration() {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    private fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a property declared in a constructor that is narrower than its type")
        fun propertyOfNarrowConstructor() {
            // The constructor's visibility decides who may build the type, not who may read the
            // property. `id` is as public as `Key`, so its KDoc belongs where it is.
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    public class Key private constructor(
                        /**
                         * The identifier.
                         *
                         * @since 1.0.0
                         */
                        public val id: String
                    )
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a protected member when the project documents them")
        fun protectedWhenAllowed() {
            val rule = KDocOnNonPublicDeclaration(TestConfig("allowProtected" to true))

            val findings = rule.lint(
                """
                    public open class Exchanger {
                        /**
                         * Exchanges keys.
                         *
                         * @since 1.0.0
                         */
                        protected fun exchange(): Unit = Unit
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
}
