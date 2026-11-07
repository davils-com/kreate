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
package com.davils.kreate.provenance.typosquat

import kotlin.math.abs

internal fun boundedEditDistance(first: String, second: String, limit: Int): Int {
    if (abs(first.length - second.length) > limit) return limit + 1
    var previous = IntArray(second.length + 1) { index -> index }
    for (row in 1..first.length) {
        val current = IntArray(second.length + 1)
        current[0] = row
        for (column in 1..second.length) {
            val substitution = previous[column - 1] + costOf(first[row - 1], second[column - 1])
            current[column] = minOf(previous[column] + 1, current[column - 1] + 1, substitution)
        }
        if (current.min() > limit) return limit + 1
        previous = current
    }
    return previous[second.length].coerceAtMost(limit + 1)
}

private fun costOf(first: Char, second: Char): Int {
    if (first == second) return 0
    return 1
}
