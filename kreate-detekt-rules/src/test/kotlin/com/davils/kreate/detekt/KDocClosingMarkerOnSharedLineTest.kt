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
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the rule that puts a KDoc block's closing marker on a line of its own.
 *
 * The snippets are assembled from pieces rather than written out, because a literal closing marker
 * inside this file's own documentation would end the comment that explains it.
 */
@DisplayName("KDocClosingMarkerOnSharedLine")
class KDocClosingMarkerOnSharedLineTest {

    @Test
    @DisplayName("reports a marker sharing a line with a tag")
    fun markerAfterTag() {
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
        findings.single().message shouldContain "@since 1.0.0"
    }

    @Test
    @DisplayName("reports a marker sharing a line with a description")
    fun markerAfterDescription() {
        val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
            """
                /**
                 * A key exchanger. $CLOSING
                public class Exchanger
            """.trimIndent()
        )

        findings shouldHaveSize 1
    }

    @Test
    @DisplayName("leaves a marker on its own line alone")
    fun markerOnOwnLine() {
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

    @Test
    @DisplayName("leaves a line holding decoration only alone")
    fun decorationOnlyLine() {
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

    @Test
    @DisplayName("leaves a one-line block alone, which has no line to move the marker to")
    fun singleLineBlock() {
        val findings = KDocClosingMarkerOnSharedLine(Config.empty).lint(
            """
                /** A key exchanger. $CLOSING
                public class Exchanger
            """.trimIndent()
        )

        findings shouldHaveSize 0
    }

    private companion object {
        /**
         * The two characters that close a KDoc block, spelled out so this file can mention them.
         */
        private const val CLOSING: String = "*/"
    }
}
