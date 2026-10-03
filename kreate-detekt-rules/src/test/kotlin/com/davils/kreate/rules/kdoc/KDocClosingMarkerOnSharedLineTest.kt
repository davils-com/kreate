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
import io.kotest.matchers.string.shouldContain

private const val CLOSING: String = "*/"

class KDocClosingMarkerOnSharedLineTest : FunSpec({

    context("reports") {
        test("a marker sharing a line with a tag") {
            val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     *
                     * @since 1.0.0 $CLOSING
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "@since 1.0.0"
        }

        test("a marker sharing a line with a description") {
            val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
                """
                    /**
                     * A key exchanger. $CLOSING
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }
    }

    context("leaves alone") {
        test("a marker on its own line") {
            val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     *
                     * @since 1.0.0
                     $CLOSING
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a line holding decoration only") {
            val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
                """
                    /**
                     * A key exchanger.
                     * $CLOSING
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a one-line block, which has no line to move the marker to") {
            val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
                """
                    /** A key exchanger. $CLOSING
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
