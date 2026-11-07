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
package com.davils.kreate.governance.catalog

private const val MAX_DIFFED_LINES: Int = 5000

internal object TextDiff {
    fun unified(path: String, before: String, after: String): String {
        if (before == after) return ""
        val old = before.lines()
        val new = after.lines()
        val body = changesOrWhole(old, new)
        return (listOf("--- a/$path", "+++ b/$path") + body).joinToString("\n", postfix = "\n")
    }

    private fun changesOrWhole(old: List<String>, new: List<String>): List<String> {
        if (old.size > MAX_DIFFED_LINES || new.size > MAX_DIFFED_LINES) {
            return old.map { line -> "-$line" } + new.map { line -> "+$line" }
        }
        return changesOf(old, new)
    }

    private fun changesOf(old: List<String>, new: List<String>): List<String> {
        val common = Array(old.size + 1) { IntArray(new.size + 1) }
        for (first in old.indices.reversed()) {
            for (second in new.indices.reversed()) {
                common[first][second] = nextLength(old, new, common, first, second)
            }
        }
        val changes = mutableListOf<String>()
        var first = 0
        var second = 0
        while (first < old.size || second < new.size) {
            val step = stepOf(old, new, common, first, second)
            changes += linesOf(step, old, new, first, second)
            if (step != Step.ADDED) first += 1
            if (step != Step.REMOVED) second += 1
        }
        return changes
    }

    private fun linesOf(step: Step, old: List<String>, new: List<String>, first: Int, second: Int): List<String> =
        when (step) {
            Step.SAME -> emptyList()
            Step.REMOVED -> listOf("@@ -${first + 1} @@", "-${old[first]}")
            Step.ADDED -> listOf("@@ +${second + 1} @@", "+${new[second]}")
        }

    private fun nextLength(
        old: List<String>,
        new: List<String>,
        common: Array<IntArray>,
        first: Int,
        second: Int
    ): Int {
        if (old[first] == new[second]) return common[first + 1][second + 1] + 1
        return maxOf(common[first + 1][second], common[first][second + 1])
    }

    private fun stepOf(
        old: List<String>,
        new: List<String>,
        common: Array<IntArray>,
        first: Int,
        second: Int
    ): Step {
        val bothLeft = first < old.size && second < new.size
        if (bothLeft && old[first] == new[second]) return Step.SAME
        if (second >= new.size) return Step.REMOVED
        if (first < old.size && common[first + 1][second] >= common[first][second + 1]) return Step.REMOVED
        return Step.ADDED
    }

    private enum class Step { SAME, REMOVED, ADDED }
}
