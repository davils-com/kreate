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
package com.davils.kreate.changelog.document

private val RELEASE: Regex = Regex("""^\[?([^\]\s]+)]?(?:\s+-\s+(\S+))?\s*$""")

private val UNRELEASED: Regex = Regex("""^\[?unreleased]?$""", RegexOption.IGNORE_CASE)

private const val LEVEL_TWO: String = "## "

private const val FENCE: String = "```"

internal class ChangelogDocument private constructor(
    private val lines: List<String>,
    val headings: List<ChangelogHeading>
) {
    fun find(version: String): ChangelogHeading? = headings.firstOrNull { heading -> heading.version == version }

    fun body(heading: ChangelogHeading): String {
        val following = headings.firstOrNull { other -> other.line > heading.line }
        val end = following?.line?.minus(1) ?: lines.size
        val section = lines.subList(heading.line, end)
        return section.joinToString("\n").trim()
    }

    companion object {
        fun parse(text: String): ChangelogDocument {
            val lines = text.replace("\r\n", "\n").split('\n')
            val fenced = fencedLines(lines)
            val headings = lines.indices.filter { index -> index !in fenced && lines[index].startsWith(LEVEL_TWO) }
            return ChangelogDocument(lines, headings.map { index -> headingOf(index + 1, lines[index]) })
        }

        private fun headingOf(line: Int, raw: String): ChangelogHeading {
            val text = raw.removePrefix(LEVEL_TWO).trim()
            if (UNRELEASED.matches(text)) return ChangelogHeading(line, text, null, null, true)
            val release = RELEASE.matchEntire(text) ?: return ChangelogHeading(line, text, null, null, false)
            val candidate = release.groupValues[1]
            val version = candidate.takeIf { value -> value.firstOrNull()?.isDigit() == true }
            return ChangelogHeading(line, text, version, release.groupValues[2].ifEmpty { null }, false)
        }

        private fun fencedLines(lines: List<String>): Set<Int> {
            val fenced = mutableSetOf<Int>()
            var inside = false
            lines.forEachIndexed { index, line ->
                val isFence = line.trimStart().startsWith(FENCE)
                if (inside || isFence) fenced += index
                if (isFence) inside = !inside
            }
            return fenced
        }
    }
}
