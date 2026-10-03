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

class ForbiddenBlockCommentTest : FunSpec({

    context("reports") {
        test("a block comment inside a function") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    public fun run() {
                        /* Retry once. */
                        attempt()
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a block comment above a declaration") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    package com.example

                    /* Exchanges keys. */
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a copyright block that is not at the top of the file") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    package com.example

                    /*
                     * Copyright 2026 Davils
                     */
                    public fun run(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a first block comment that is no license header") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    /* Utilities for exchanging keys. */
                    package com.example
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("the license header when the project accepts none") {
            val rule = ForbiddenBlockComment(TestConfig("licenseHeaderPattern" to ""))

            val findings = rule.lint(
                """
                    /*
                     * Copyright 2026 Davils
                     */
                    package com.example
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a KDoc block inside a function body") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    public fun run() {
                        /** Retry once. */
                        attempt()
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a KDoc block at the end of a class body") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    public class Exchanger {
                        public fun run(): Unit = Unit

                        /** Nothing follows. */
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a second KDoc block above a documented declaration") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    /** Exchanges keys. */
                    /** Exchanges keys again. */
                    public fun exchange(): Unit = Unit
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }
    }

    context("leaves alone") {
        test("the license header at the top of the file") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    /*
                     * Copyright 2026 Davils
                     *
                     *     http://www.apache.org/licenses/LICENSE-2.0
                     */

                    package com.example
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("the license header after a byte order mark") {
            val header = """
                /*
                 * Copyright 2026 Davils
                 */

                package com.example
            """.trimIndent()

            val findings = ForbiddenBlockComment(Config.empty).lint("\uFEFF$header")

            findings shouldHaveSize 0
        }

        test("KDoc blocks on a member, a constructor property and an annotated function") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    public class Key(
                        /** The identifier. */
                        public val id: String
                    ) {
                        /** Exchanges the key. */
                        public fun exchange(): Unit = Unit

                        @Deprecated("Use exchange.")
                        /** Exchanges the key the old way. */
                        public fun swap(): Unit = Unit
                    }
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a KDoc block") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
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

        test("a block comment marker inside a string literal") {
            val findings = ForbiddenBlockComment(Config.empty).lint(
                """
                    public val glob: String = "src/**/*.kt"
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a header matching a configured pattern") {
            val rule = ForbiddenBlockComment(TestConfig("licenseHeaderPattern" to "SPDX-License-Identifier"))

            val findings = rule.lint(
                """
                    /* SPDX-License-Identifier: Apache-2.0 */
                    package com.example
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
