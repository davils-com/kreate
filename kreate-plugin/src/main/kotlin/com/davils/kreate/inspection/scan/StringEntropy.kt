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

import kotlin.math.ln

private const val MIN_LENGTH: Int = 20

private const val MAX_LENGTH: Int = 4096

private const val MEDIUM_ENTROPY: Double = 4.5

private const val ASCENDING_RUN: Int = 6

private val TOKEN_ALPHABET: Regex = Regex("[A-Za-z0-9+/=_-]+")

internal object StringEntropy {
    fun isSuspicious(text: String): Boolean {
        if (text.length !in MIN_LENGTH..MAX_LENGTH) return false
        if (!TOKEN_ALPHABET.matches(text)) return false
        if (text.none { character -> character.isDigit() }) return false
        if (hasAscendingRun(text)) return false
        return shannonEntropyOf(text) > MEDIUM_ENTROPY
    }

    fun shannonEntropyOf(text: String): Double {
        val counts = text.groupingBy { character -> character }.eachCount()
        val length = text.length.toDouble()
        val sum = counts.values.sumOf { count ->
            val probability = count / length
            -probability * ln(probability)
        }
        return sum / ln(2.0)
    }

    private fun nextRun(run: Int, isNext: Boolean): Int {
        if (isNext) return run + 1
        return 1
    }

    private fun hasAscendingRun(text: String): Boolean {
        val ascending = text.zipWithNext { first, second -> second == first + 1 }
        val runs = ascending.runningFold(1) { run, isNext -> nextRun(run, isNext) }
        return runs.any { run -> run >= ASCENDING_RUN }
    }
}
