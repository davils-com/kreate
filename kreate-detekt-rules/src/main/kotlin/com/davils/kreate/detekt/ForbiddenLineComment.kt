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

import com.intellij.psi.PsiComment
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Reports every `//` comment, whether it stands on its own line or trails code.
 *
 * The Kreate code style asks for a name or a smaller function where a line comment would go. The
 * reason is not aesthetic: a comment is not compiled, not tested and not renamed with the thing it
 * describes, so it is the one part of a file that can be wrong without anything failing. A name is
 * checked by the compiler on every build.
 *
 * Detekt's own `ForbiddenComment` can be pointed at patterns such as `TODO:`; it cannot express
 * "none at all", and it treats KDoc and block comments the same way. This rule is about the `//`
 * form alone, which leaves the copyright header and every KDoc block untouched.
 *
 * @since 3.3.0
 */
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

        val content = comment.text.removePrefix(LINE_COMMENT_MARKER).trim()
        if (isAllowed(content)) return

        report(
            Finding(
                Entity.from(comment),
                "This line comment has to go: rename what it explains, or split the function it " +
                    "explains. Documentation of a public declaration belongs in a KDoc block."
            )
        )
    }

    /**
     * Whether the configured exception covers this comment.
     *
     * An empty pattern means "allow nothing", so it is checked for explicitly: an empty regular
     * expression matches every string, which would switch the rule off instead of tightening it.
     *
     * @param content The comment's text, without the marker.
     * @return `true` when the comment must not be reported.
     * @since 3.3.0
     */
    private fun isAllowed(content: String): Boolean =
        allowedPattern.pattern.isNotEmpty() && allowedPattern.containsMatchIn(content)

    private companion object {
        /**
         * The characters a line comment opens with.
         *
         * @since 3.3.0
         */
        private const val LINE_COMMENT_MARKER: String = "//"
    }
}
