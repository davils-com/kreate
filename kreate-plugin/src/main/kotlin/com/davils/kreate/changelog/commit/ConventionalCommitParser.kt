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
package com.davils.kreate.changelog.commit

import com.davils.kreate.vcs.GitCommit

private val HEADER: Regex = Regex("""^([A-Za-z][A-Za-z0-9-]*)(?:\(([^()\r\n]+)\))?(!)?: (\S.*)$""")

private val GIT_REVERT: Regex = Regex("""^Revert "(.+)"$""")

private val REVERTED_COMMIT: Regex = Regex("This reverts commit ([0-9a-fA-F]{7,64})")

private val BREAKING_FOOTER: Regex = Regex("^BREAKING[ -]CHANGE: ?(.*)$")

private val FOOTER: Regex = Regex("^(?:BREAKING[ -]CHANGE|[A-Za-z][A-Za-z0-9-]*)(?:: | #)")

private const val TYPE: Int = 1

private const val SCOPE: Int = 2

private const val BANG: Int = 3

private const val DESCRIPTION: Int = 4

internal object ConventionalCommitParser {
    fun parse(commit: GitCommit): ConventionalCommit {
        val lines = commit.message.replace("\r\n", "\n").split('\n')
        val subject = lines.first().trim()
        val body = lines.drop(1)
        val notes = breakingNotesOf(body)
        val matches = REVERTED_COMMIT.findAll(body.joinToString("\n"))
        val reverted = matches.map { match -> match.groupValues[1].lowercase() }.toList()
        val revert = GIT_REVERT.matchEntire(subject)
        if (revert != null) return revertOf(commit.hash, revert.groupValues[1], notes, reverted)

        val header = HEADER.matchEntire(subject)
            ?: return ConventionalCommit(commit.hash, null, subject, revertedHashes = reverted)
        val values = header.groupValues
        return ConventionalCommit(
            hash = commit.hash,
            type = values[TYPE].lowercase(),
            description = values[DESCRIPTION].trim(),
            scope = values[SCOPE].trim().ifEmpty { null },
            breaking = values[BANG].isNotEmpty() || notes.isNotEmpty(),
            breakingNotes = notes,
            revertedHashes = reverted
        )
    }

    private fun revertOf(
        hash: String,
        subject: String,
        notes: List<String>,
        reverted: List<String>
    ): ConventionalCommit {
        val values = HEADER.matchEntire(subject)?.groupValues
        val description = values?.get(DESCRIPTION)?.trim() ?: subject
        val scope = values?.get(SCOPE)?.trim()
        return ConventionalCommit(
            hash = hash,
            type = "revert",
            description = "Revert \"$description\"",
            scope = scope?.ifEmpty { null },
            breaking = notes.isNotEmpty(),
            breakingNotes = notes,
            revertedHashes = reverted
        )
    }

    private fun breakingNotesOf(body: List<String>): List<String> {
        val notes = mutableListOf<String>()
        var current: StringBuilder? = null
        body.forEach { line ->
            val breaking = BREAKING_FOOTER.matchEntire(line)
            val isFooter = FOOTER.containsMatchIn(line)
            if (breaking != null || isFooter) current?.let { note -> addNote(notes, note) }
            if (breaking != null) current = StringBuilder(breaking.groupValues[1])
            if (breaking == null && isFooter) current = null
            if (breaking == null && !isFooter) current?.append('\n')?.append(line)
        }
        current?.let { note -> addNote(notes, note) }
        return notes
    }

    private fun addNote(notes: MutableList<String>, note: StringBuilder) {
        val text = note.toString().trim()
        if (text.isNotEmpty()) notes += text
    }
}
