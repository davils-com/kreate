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
package com.davils.kreate.assurance.report

import com.davils.kore.core.text.SafeText

private const val MAX_SNIPPET_LENGTH: Int = 160

private val COMBINING_MARKS: Set<Int> = setOf(
    Character.NON_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt()
)

internal object SnippetSanitizer {
    fun sanitize(text: String): String = SafeText.text(text.trim(), MAX_SNIPPET_LENGTH)

    fun escape(text: String): String {
        val escaped = StringBuilder(text.length)
        var isAttached = false
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val piece = pieceOf(codePoint, isAttached)
            escaped.append(piece)
            val width = Character.charCount(codePoint)
            isAttached = SafeText.isSafe(codePoint) && piece.length == width
            index += width
        }
        return escaped.toString()
    }

    private fun pieceOf(codePoint: Int, isAttached: Boolean): String {
        val character = String(Character.toChars(codePoint))
        val isLooseMark = !isAttached && Character.getType(codePoint) in COMBINING_MARKS
        if (isLooseMark) return SafeText.text(character)
        if (SafeText.isSafe(codePoint)) return character
        return SafeText.text(character)
    }
}
