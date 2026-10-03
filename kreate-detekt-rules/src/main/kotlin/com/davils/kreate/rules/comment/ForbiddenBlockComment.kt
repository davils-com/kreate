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
import com.davils.kreate.rules.kdoc.kDocBlocks
import com.intellij.psi.PsiComment
import dev.detekt.api.ActiveByDefault
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtFile

@ActiveByDefault(since = "4.0.0")
internal class ForbiddenBlockComment(config: Config) : Rule(
    config,
    "A block comment says in prose what the code should have said in a name.",
    RULE_DOCUMENTATION
) {

    @Configuration(
        "Regular expression the block comment at the very top of a file has to contain to be " +
            "accepted as its license header. Empty accepts no header at all."
    )
    private val licenseHeaderPattern: Regex by config(DEFAULT_LICENSE_HEADER) { pattern -> Regex(pattern) }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)

        if (comment.tokenType != KtTokens.BLOCK_COMMENT) return
        if (isLicenseHeader(comment)) return

        report(
            Finding(
                Entity.from(comment),
                "This block comment has to go: rename what it explains, or split the function it " +
                    "explains. Documentation of a public declaration belongs in a KDoc block."
            )
        )
    }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val blocks = file.kDocBlocks()
        val detached = blocks.filterNot { documentation -> documentation.documentsDeclaration() }
        detached.forEach { documentation -> reportDetached(documentation) }
    }

    private fun reportDetached(documentation: KDoc) {
        if (isLicenseHeader(documentation)) return

        report(
            Finding(
                Entity.from(documentation),
                "This KDoc block documents no declaration, which makes it a block comment: rename what " +
                    "it explains, or move it directly above the declaration it documents."
            )
        )
    }

    private fun KDoc.documentsDeclaration(): Boolean {
        val declaration = documentedDeclaration() ?: return false
        return declaration.docComment == this
    }

    private fun KDoc.documentedDeclaration(): KtDeclaration? {
        val holder = parent
        if (holder is KtDeclarationModifierList) return holder.parent as? KtDeclaration
        return holder as? KtDeclaration
    }

    private fun isLicenseHeader(comment: PsiComment): Boolean {
        val hasPattern = licenseHeaderPattern.pattern.isNotEmpty()
        if (!hasPattern) return false
        if (!comment.isFirstInFile()) return false
        return licenseHeaderPattern.containsMatchIn(comment.text)
    }

    private fun PsiComment.isFirstInFile(): Boolean {
        val fileText = containingFile.text
        val firstContent = fileText.indexOfFirst { character -> character.isContent() }
        return textRange.startOffset == firstContent
    }

    private fun Char.isContent(): Boolean = !isWhitespace() && this != BYTE_ORDER_MARK

    private companion object {
        private const val DEFAULT_LICENSE_HEADER: String = "Copyright"
        private const val BYTE_ORDER_MARK: Char = '\uFEFF'
    }
}
