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

import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFile

/**
 * Reports a multi-line KDoc block that closes on a line which still holds content.
 *
 * A block whose last line reads `* @since 1.0.0` followed by the closing marker puts the version
 * and the end of the comment into one token as far as a reader skimming a diff is concerned, and
 * the next tag added to the block has to move the marker anyway. A closing marker on its own line
 * makes the block's extent obvious and keeps every later edit to a single line.
 *
 * A one-line comment is not reported: there the marker has no line of its own to move to.
 *
 * @since 3.3.0
 */
@ActiveByDefault(since = "3.3.0")
internal class KDocClosingMarkerOnSharedLine(config: Config) : Rule(
    config,
    "The closing marker of a multi-line KDoc block belongs on a line of its own.",
    RULE_DOCUMENTATION
) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file.kDocBlocks().forEach { documentation -> check(documentation) }
    }

    /**
     * Reports the block when its closing marker shares a line with content.
     *
     * @param documentation The block to check.
     * @since 3.3.0
     */
    private fun check(documentation: KDoc) {
        if (documentation.isSingleLine) return

        val lastLine = documentation.contentBeforeClosingMarker() ?: return
        val content = lastLine.withoutDecoration()
        if (content.isEmpty()) return

        report(
            Finding(
                Entity.from(documentation),
                "Move the closing '$KDOC_CLOSING' onto its own line. It currently shares a line " +
                    "with '$content'."
            )
        )
    }

    /**
     * The line's own text, with the asterisk every KDoc line is indented by removed.
     *
     * Without this, a decoration-only line such as `*` counts as content and the rule reports a
     * block that is already formatted the way it asks for.
     *
     * @return The text, or an empty string when the line carries decoration only.
     * @since 3.3.0
     */
    private fun String.withoutDecoration(): String = trim().removePrefix("*").trim()
}
