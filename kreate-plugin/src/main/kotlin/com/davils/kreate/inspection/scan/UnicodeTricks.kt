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
package com.davils.kreate.inspection.scan

private val BIDI_RANGES: List<IntRange> = listOf(0x061C..0x061C, 0x200E..0x200F, 0x202A..0x202E, 0x2066..0x2069)

private val ZERO_WIDTH_RANGES: List<IntRange> = listOf(0x034F..0x034F, 0x180E..0x180E, 0x200B..0x200D, 0x2060..0x2064)

private val TAG_RANGE: IntRange = 0xE0000..0xE007F

private val PRIVATE_USE_RANGES: List<IntRange> = listOf(0xE000..0xF8FF, 0xF0000..0x10FFFF)

private val LOOK_ALIKES: Set<Int> = setOf(
    0x0391, 0x0392, 0x0395, 0x0396, 0x0397, 0x0399, 0x039A, 0x039C, 0x039D, 0x039F, 0x03A1, 0x03A4, 0x03A5, 0x03A7,
    0x03B1, 0x03B5, 0x03B9, 0x03BA, 0x03BD, 0x03BF, 0x03C1, 0x03C5, 0x0405, 0x0406, 0x0408, 0x0410, 0x0412, 0x0415,
    0x041A, 0x041C, 0x041D, 0x041E, 0x0420, 0x0421, 0x0422, 0x0425, 0x0430, 0x0435, 0x043E, 0x0440, 0x0441, 0x0443,
    0x0445, 0x0455, 0x0456, 0x0458, 0x04BB, 0x04CF, 0x0501, 0x051B, 0x051D
)

private val WORD: Regex = Regex("""[\p{L}\p{N}_]+""")

private const val MAX_SCANNED_CHARACTERS: Int = 16 * 1024

private const val BYTE_ORDER_MARK: Int = 0xFEFF

internal object UnicodeTricks {
    fun scan(text: String, path: String, hits: MutableCollection<Hit>) {
        val bounded = text.take(MAX_SCANNED_CHARACTERS)
        val codePoints = bounded.codePoints().toArray()

        if (codePoints.any { point -> BIDI_RANGES.any { range -> point in range } }) hits += Hit("BIDI_OVERRIDE", path)
        if (hasHiddenCharacter(codePoints)) hits += Hit("ZERO_WIDTH", path)
        if (codePoints.any { point -> point in TAG_RANGE }) hits += Hit("TAG_CHARACTER", path)
        if (hasPrivateUse(codePoints)) hits += Hit("PRIVATE_USE_CHARACTER", path)
        if (WORD.findAll(bounded).any { word -> isHomoglyph(word.value) }) hits += Hit("HOMOGLYPH", path)
    }

    private fun hasHiddenCharacter(codePoints: IntArray): Boolean = codePoints.withIndex().any { (index, point) ->
        val isByteOrderMark = point == BYTE_ORDER_MARK && index > 0
        isByteOrderMark || ZERO_WIDTH_RANGES.any { range -> point in range }
    }

    private fun hasPrivateUse(codePoints: IntArray): Boolean =
        codePoints.any { point -> PRIVATE_USE_RANGES.any { range -> point in range } }

    private fun isHomoglyph(word: String): Boolean {
        val hasLatin = word.any { character -> character in 'a'..'z' || character in 'A'..'Z' }
        return hasLatin && word.codePoints().anyMatch { point -> point in LOOK_ALIKES }
    }
}
