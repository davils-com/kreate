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

package com.davils.kreate.rules.kdoc

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

class KDocOnNonPublicDeclarationTest : FunSpec({

    context("reports") {
        test("a private function") {
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

        test("an internal class") {
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

        test("a public member of an internal class, which no consumer can reach") {
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

        test("a declaration inside a function body") {
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

        test("a protected member, by default") {
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

    context("leaves alone") {
        test("a public declaration") {
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

        test("a declaration without documentation") {
            val findings = KDocOnNonPublicDeclaration(Config.empty).lint(
                """
                    private fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a public property of a type whose constructor is private") {
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

        test("a protected member when the project documents them") {
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
})
