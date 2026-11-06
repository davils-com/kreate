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

package com.davils.kreate.json

private const val UNICODE_ESCAPE_WIDTH: Int = 4

private const val HEX_RADIX: Int = 16

private val SHORT_ESCAPES: Map<Char, String> = mapOf(
    '"' to "\\\"",
    '\\' to "\\\\",
    '\b' to "\\b",
    '\u000C' to "\\f",
    '\n' to "\\n",
    '\r' to "\\r",
    '\t' to "\\t"
)

internal object JsonStringEscaper {
    fun quote(text: String): String = buildString {
        append('"')
        text.indices.forEach { index -> append(escapeAt(text, index) ?: text[index]) }
        append('"')
    }

    private fun escapeAt(text: String, index: Int): String? {
        val character = text[index]
        val shortEscape = SHORT_ESCAPES[character]
        if (shortEscape != null) return shortEscape

        val needsUnicodeEscape = character < ' ' || isLoneSurrogate(text, index)
        if (!needsUnicodeEscape) return null

        val digits = character.code.toString(HEX_RADIX)
        return "\\u" + digits.padStart(UNICODE_ESCAPE_WIDTH, '0')
    }

    private fun isLoneSurrogate(text: String, index: Int): Boolean {
        val character = text[index]
        if (character.isHighSurrogate()) {
            val next = text.getOrNull(index + 1)
            return next == null || !next.isLowSurrogate()
        }
        if (!character.isLowSurrogate()) return false

        val previous = text.getOrNull(index - 1)
        return previous == null || !previous.isHighSurrogate()
    }
}
