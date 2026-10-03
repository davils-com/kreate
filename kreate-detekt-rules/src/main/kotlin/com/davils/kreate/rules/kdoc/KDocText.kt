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

import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

internal const val KDOC_OPENING: String = "/**"

internal const val KDOC_CLOSING: String = "*/"

private val BLOCK_TAG: Regex = Regex("""(?<=\s)@[A-Za-z]\w*""")

internal fun KtFile.kDocBlocks(): List<KDoc> = collectDescendantsOfType<KDoc>()

internal val KDoc.isSingleLine: Boolean get() = !text.contains('\n')

internal fun KDoc.blockTags(): List<MatchResult> {
    val matches = BLOCK_TAG.findAll(text)
    return matches.toList()
}

internal fun KDoc.hasBlockTag(tag: String): Boolean {
    val tags = blockTags()
    return tags.any { match -> match.value == "@$tag" }
}

internal fun KDoc.singleLineDescription(): String? {
    if (!isSingleLine) return null
    val firstTag = blockTags().firstOrNull() ?: return null
    val description = text.substring(KDOC_OPENING.length, firstTag.range.first)
    return description.trim()
}

internal fun KDoc.contentBeforeClosingMarker(): String? {
    if (!text.endsWith(KDOC_CLOSING)) return null
    val lastLine = text.substringAfterLast('\n')
    return lastLine.removeSuffix(KDOC_CLOSING)
}
