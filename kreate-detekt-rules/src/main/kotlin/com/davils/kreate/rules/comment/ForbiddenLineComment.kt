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

import com.davils.kreate.rules.RULE_DOCUMENTATION
import com.intellij.psi.PsiComment
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens

@ActiveByDefault(since = "3.3.0")
internal class ForbiddenLineComment(config: Config) : Rule(
    config,
    "A line comment says in prose what the code should have said in a name.",
    RULE_DOCUMENTATION
) {

    @Configuration(
        "Regular expression a line comment may match to be allowed. Empty allows none, which is " +
            "the standard; a project that keeps directives such as 'region' in its sources can " +
            "name them here."
    )
    private val allowedPattern: Regex by config("") { pattern -> Regex(pattern) }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)

        if (comment.tokenType != KtTokens.EOL_COMMENT) return

        val withoutMarker = comment.text.removePrefix(LINE_COMMENT_MARKER)
        val content = withoutMarker.trim()
        if (isAllowed(content)) return

        report(
            Finding(
                Entity.from(comment),
                "This line comment has to go: rename what it explains, or split the function it " +
                    "explains. Documentation of a public declaration belongs in a KDoc block."
            )
        )
    }

    private fun isAllowed(content: String): Boolean {
        val hasPattern = allowedPattern.pattern.isNotEmpty()
        return hasPattern && allowedPattern.containsMatchIn(content)
    }

    private companion object {
        private const val LINE_COMMENT_MARKER: String = "//"
    }
}
