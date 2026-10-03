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

class SingleLineKDocWithBlockTagTest : FunSpec({

    context("reports") {
        test("a description followed by a tag on one line") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger. @since 1.0.0 */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a description followed by several tags on one line") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** Exchanges keys. @param peer The peer. @since 1.0.0 */
                    public fun exchange(peer: String): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }
    }

    context("leaves alone") {
        test("a one-line block holding a description only") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a one-line block holding a tag only") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** @since 1.0.0 */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a multi-line block, where the tag is a tag") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
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

        test("an inline reference, which is not a block tag") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger, see {@link Key}. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("an email address in the description") {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger, owned by team@example.com. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
