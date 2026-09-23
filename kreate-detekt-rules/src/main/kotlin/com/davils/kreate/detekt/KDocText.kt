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

import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * The marker a KDoc block opens with.
 *
 * @since 3.3.0
 */
internal const val KDOC_OPENING: String = "/**"

/**
 * The marker a KDoc block closes with.
 *
 * @since 3.3.0
 */
internal const val KDOC_CLOSING: String = "*/"

/**
 * A block tag such as `@param`, `@return` or `@since`.
 *
 * Matched on the comment's text rather than through the KDoc PSI, and that is the whole point of
 * this file. The KDoc lexer only recognises a tag at the start of a line, so in
 * `/** A key exchanger. @since 1.0.0 */` the `@since` is plain prose as far as the PSI is
 * concerned — which is exactly the shape two of these rules exist to find.
 *
 * The lookbehind is what keeps `{@link Key}` and `nils@example.com` out: a block tag is preceded by
 * whitespace, an inline tag by a brace and an address by the local part.
 *
 * @since 3.3.0
 */
private val BLOCK_TAG: Regex = Regex("""(?<=\s)@[A-Za-z]\w*""")

/**
 * Every KDoc block in the file, attached to a declaration or not.
 *
 * Collected by walking the file rather than by overriding `visitComment`. Kotlin's tree visitor does
 * not dispatch a KDoc block there — it is a `KtElement` in its own right, not one of the comment
 * tokens — so a rule that waited for `visitComment` would quietly see nothing. A rule that only
 * cares about documentation which is attached to a declaration reads `KtDeclaration.docComment`
 * instead; the two rules about a block's shape have to see the detached ones as well, because being
 * detached is part of what they report.
 *
 * @return The blocks, in source order.
 * @since 3.3.0
 */
internal fun KtFile.kDocBlocks(): List<KDoc> = collectDescendantsOfType<KDoc>()

/**
 * Whether the whole comment sits on one line.
 *
 * @since 3.3.0
 */
internal val KDoc.isSingleLine: Boolean get() = !text.contains('\n')

/**
 * Every block tag in the comment, in the order they appear.
 *
 * @return The matches, each holding the tag's name and its position in [KDoc.getText].
 * @since 3.3.0
 */
internal fun KDoc.blockTags(): List<MatchResult> = BLOCK_TAG.findAll(text).toList()

/**
 * Whether the comment carries the given block tag.
 *
 * @param tag The tag's name, without the `@`.
 * @return `true` when the tag is present.
 * @since 3.3.0
 */
internal fun KDoc.hasBlockTag(tag: String): Boolean = blockTags().any { match -> match.value == "@$tag" }

/**
 * The description a single-line comment holds before its first block tag.
 *
 * Only defined for a single-line comment: on a multi-line one the same span would also contain the
 * leading asterisks of every line it crosses, and nothing here needs that.
 *
 * @return The description, or `null` when the comment spans several lines or carries no block tag.
 * @since 3.3.0
 */
internal fun KDoc.singleLineDescription(): String? {
    if (!isSingleLine) return null
    val firstTag = blockTags().firstOrNull() ?: return null
    return text.substring(KDOC_OPENING.length, firstTag.range.first).trim()
}

/**
 * What the comment's last line holds before its closing marker.
 *
 * @return The text of the final line up to the closing marker, or `null` when the comment is
 * unterminated.
 * @since 3.3.0
 */
internal fun KDoc.contentBeforeClosingMarker(): String? {
    if (!text.endsWith(KDOC_CLOSING)) return null
    val lastLine = text.substringAfterLast('\n')
    return lastLine.removeSuffix(KDOC_CLOSING)
}
