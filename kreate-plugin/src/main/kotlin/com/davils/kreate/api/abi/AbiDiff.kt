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

package com.davils.kreate.api.abi

internal object AbiDiff {
    private const val MAX_REPORTED_LINES = 60

    fun render(expected: String, actual: String): String? {
        if (expected == actual) return null

        val expectedLines = expected.lines()
        val actualLines = actual.lines()

        var start = 0
        val maxStart = minOf(expectedLines.size, actualLines.size)
        while (start < maxStart && expectedLines[start] == actualLines[start]) start++

        var fromEnd = 0
        val maxFromEnd = minOf(expectedLines.size, actualLines.size) - start
        while (
            fromEnd < maxFromEnd &&
            expectedLines[expectedLines.size - 1 - fromEnd] == actualLines[actualLines.size - 1 - fromEnd]
        ) {
            fromEnd++
        }

        val removed = expectedLines.subList(start, expectedLines.size - fromEnd)
        val added = actualLines.subList(start, actualLines.size - fromEnd)

        return buildString {
            appendLine("--- ${start + 1} line(s) of context skipped")
            appendSection("-", removed)
            appendSection("+", added)
        }.trimEnd()
    }

    private fun StringBuilder.appendSection(prefix: String, lines: List<String>) {
        lines.take(MAX_REPORTED_LINES).forEach { line -> appendLine("$prefix$line") }
        if (lines.size > MAX_REPORTED_LINES) {
            appendLine("$prefix... and ${lines.size - MAX_REPORTED_LINES} more line(s)")
        }
    }
}
