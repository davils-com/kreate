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
package com.davils.kreate.changelog.build

private const val SHORT_HASH: Int = 7

internal object ChangelogRenderer {
    fun render(version: String, date: String?, changes: ChangelogChanges): String {
        val heading = date?.let { day -> "## $version - $day" } ?: "## $version"
        val sections = changes.sections.entries.map { (section, entries) -> sectionOf(section, entries) }
        val body = sections.ifEmpty { listOf("No notable changes.") }
        return (listOf(heading) + body).joinToString("\n\n", postfix = "\n")
    }

    private fun sectionOf(section: ChangelogSection, entries: List<ChangelogEntry>): String {
        val lines = entries.map(::lineOf)
        return "### ${section.heading}\n\n" + lines.joinToString("\n")
    }

    private fun lineOf(entry: ChangelogEntry): String {
        val scope = entry.scope?.let { name -> "**$name:** " }.orEmpty()
        val notes = entry.breakingNotes.joinToString("") { note -> "\n  " + note.replace("\n", "\n  ") }
        return "- $scope${entry.description} (${entry.hash.take(SHORT_HASH)})$notes"
    }
}
