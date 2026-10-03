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

import com.davils.kreate.rules.RULE_DOCUMENTATION
import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDeclaration

@ActiveByDefault(since = "3.3.0")
internal class KDocSeparatedFromDeclaration(config: Config) : Rule(
    config,
    "A KDoc block belongs directly above the declaration it documents.",
    RULE_DOCUMENTATION
) {

    override fun visitDeclaration(dcl: KtDeclaration) {
        super.visitDeclaration(dcl)

        val documentation = dcl.docComment ?: return
        val gap = documentation.nextSibling as? PsiWhiteSpace ?: return
        val lineBreaks = gap.text.count { character -> character == '\n' }
        if (lineBreaks < BLANK_LINE_NEWLINES) return

        report(
            Finding(
                Entity.from(documentation),
                "Remove the blank line between this KDoc block and the declaration it documents."
            )
        )
    }

    private companion object {
        private const val BLANK_LINE_NEWLINES: Int = 2
    }
}
