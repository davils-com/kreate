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
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize

class KDocSeparatedFromDeclarationTest : FunSpec({

    context("reports") {
        test("a blank line before a class") {
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

        test("a blank line before an annotated declaration") {
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

        test("a blank line before a property inside a class body") {
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

    context("leaves alone") {
        test("a block that sits directly above its declaration") {
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

        test("a block that documents nothing below it") {
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
})
