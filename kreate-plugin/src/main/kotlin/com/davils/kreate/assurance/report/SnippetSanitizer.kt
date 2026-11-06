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

private const val MAX_SNIPPET_LENGTH: Int = 160

private const val ELLIPSIS: String = "\u2026"

private const val HEX_RADIX: Int = 16

private const val MIN_HEX_DIGITS: Int = 4

private val UNSAFE_CODE_POINTS: List<IntRange> = listOf(
    0x0000..0x0008,
    0x000A..0x001F,
    0x007F..0x009F,
    0x00AD..0x00AD,
    0x061C..0x061C,
    0x180E..0x180E,
    0x200B..0x200F,
    0x2028..0x202E,
    0x2060..0x206F,
    0xE000..0xF8FF,
    0xFE00..0xFE0F,
    0xFEFF..0xFEFF,
    0xE0000..0xE0FFF,
    0xF0000..0x10FFFF
)

internal object SnippetSanitizer {
    fun sanitize(text: String): String {
        val escaped = escape(text).trim()
        if (escaped.length <= MAX_SNIPPET_LENGTH) return escaped
        return escaped.take(MAX_SNIPPET_LENGTH) + ELLIPSIS
    }

    fun escape(text: String): String = buildString {
        text.codePoints().forEach { codePoint -> appendSafely(codePoint) }
    }

    private fun StringBuilder.appendSafely(codePoint: Int) {
        val isUnsafe = UNSAFE_CODE_POINTS.any { range -> codePoint in range }
        if (!isUnsafe) {
            appendCodePoint(codePoint)
            return
        }

        val hex = codePoint.toString(HEX_RADIX).uppercase()
        val digits = hex.padStart(MIN_HEX_DIGITS, '0')
        append("\\u{$digits}")
    }
}
