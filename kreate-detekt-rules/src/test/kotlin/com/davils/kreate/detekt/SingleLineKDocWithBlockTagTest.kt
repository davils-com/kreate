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
 * Tests for the rule that splits a one-line KDoc holding both prose and a tag.
 *
 * The false positives worth guarding against are an inline `{@link}` reference and an email address,
 * both of which look like a tag to a naive search and neither of which is one.
 */
@DisplayName("SingleLineKDocWithBlockTag")
class SingleLineKDocWithBlockTagTest {

    @Test
    @DisplayName("reports a description followed by a tag on one line")
    fun descriptionAndTag() {
        val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
            """
                /** A key exchanger. @since 1.0.0 */
                public class Exchanger
            """.trimIndent()
        )

        findings shouldHaveSize 1
    }

    @Test
    @DisplayName("reports a description followed by several tags on one line")
    fun descriptionAndSeveralTags() {
        val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
            """
                /** Exchanges keys. @param peer The peer. @since 1.0.0 */
                public fun exchange(peer: String): Unit = Unit
            """.trimIndent()
        )

        findings shouldHaveSize 1
    }

    @Nested
    @DisplayName("leaves alone")
    inner class LeavesAlone {

        @Test
        @DisplayName("a one-line block holding a description only")
        fun descriptionOnly() {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a one-line block holding a tag only")
        fun tagOnly() {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** @since 1.0.0 */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("a multi-line block, where the tag is a tag")
        fun multiLineBlock() {
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

        @Test
        @DisplayName("an inline reference, which is not a block tag")
        fun inlineReference() {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger, see {@link Key}. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        @Test
        @DisplayName("an email address in the description")
        fun emailAddress() {
            val findings = SingleLineKDocWithBlockTag(Config.empty).lint(
                """
                    /** A key exchanger, owned by team@example.com. */
                    public class Exchanger
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
}
