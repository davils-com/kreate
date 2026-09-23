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
 * Reports a one-line KDoc block that holds a description and a block tag at once.
 *
 * `/** A key exchanger. @since 1.0.0 */` does not say what it looks like it says. The KDoc lexer
 * only recognises a tag at the start of a line, so the `@since` is rendered as part of the
 * description and no tooling ever sees a version — Dokka included. The same text over four lines
 * carries a tag.
 *
 * A one-line comment that holds only a description, or only tags, is left alone. Both are
 * unambiguous, and neither hides anything from the reader.
 *
 * @since 3.3.0
 */
@ActiveByDefault(since = "3.3.0")
internal class SingleLineKDocWithBlockTag(config: Config) : Rule(
    config,
    "A block tag on the same line as a description is read as part of the description.",
    RULE_DOCUMENTATION
) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        file.kDocBlocks().forEach { documentation -> check(documentation) }
    }

    /**
     * Reports the block when it holds a description and a block tag on the same line.
     *
     * @param documentation The block to check.
     * @since 3.3.0
     */
    private fun check(documentation: KDoc) {
        val description = documentation.singleLineDescription() ?: return
        if (description.isEmpty()) return

        report(
            Finding(
                Entity.from(documentation),
                "Spread this KDoc over several lines. A block tag is only recognised at the " +
                    "start of a line, so this one is currently part of the description."
            )
        )
    }
}
