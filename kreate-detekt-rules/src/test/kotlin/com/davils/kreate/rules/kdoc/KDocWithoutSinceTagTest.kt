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

class KDocWithoutSinceTagTest : FunSpec({

    context("reports") {
        test("a public function documented without a version") {
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

        test("a public property documented without a version") {
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

    context("leaves alone") {
        test("a block that carries the tag") {
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

        test("a single-line block that carries the tag, which the KDoc lexer does not see as one") {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    /** Exchanges keys. @since 1.0.0 */
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a non-public declaration, which KDocOnNonPublicDeclaration owns") {
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

        test("an undocumented declaration, which the comments rule set owns") {
            val findings = KDocWithoutSinceTag(Config.empty).lint(
                """
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
