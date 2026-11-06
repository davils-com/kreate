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

internal object JsonNumberGrammar {
    const val NOT_A_NUMBER: Int = -1

    private val DIGITS: CharRange = '0'..'9'

    fun matches(text: String): Boolean = endOf(text, 0) == text.length

    fun endOf(text: String, start: Int): Int {
        val integerStart = afterMinusSign(text, start)
        val integerEnd = integerEnd(text, integerStart)
        if (integerEnd == NOT_A_NUMBER) return NOT_A_NUMBER

        val fractionEnd = fractionEnd(text, integerEnd)
        if (fractionEnd == NOT_A_NUMBER) return NOT_A_NUMBER

        return exponentEnd(text, fractionEnd)
    }

    private fun afterMinusSign(text: String, start: Int): Int {
        if (text.getOrNull(start) == '-') return start + 1
        return start
    }

    private fun integerEnd(text: String, start: Int): Int {
        if (text.getOrNull(start) == '0') return start + 1
        return digitsEnd(text, start)
    }

    private fun fractionEnd(text: String, start: Int): Int {
        if (text.getOrNull(start) != '.') return start
        return digitsEnd(text, start + 1)
    }

    private fun exponentEnd(text: String, start: Int): Int {
        val marker = text.getOrNull(start)
        if (marker != 'e' && marker != 'E') return start

        val afterMarker = start + 1
        val sign = text.getOrNull(afterMarker)
        if (sign == '+' || sign == '-') return digitsEnd(text, afterMarker + 1)
        return digitsEnd(text, afterMarker)
    }

    private fun digitsEnd(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index] in DIGITS) index++

        if (index == start) return NOT_A_NUMBER
        return index
    }
}
