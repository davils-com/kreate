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

import com.davils.kreate.vcs.SemanticVersion
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val HEADING_FORMS: String = """Use "## 1.2.3 - YYYY-MM-DD", "## 1.2.3" or "## Unreleased"."""

internal object ChangelogValidator {
    fun validate(document: ChangelogDocument, version: String?): List<ChangelogProblem> {
        val problems = mutableListOf<ChangelogProblem>()
        val seen = mutableSetOf<SemanticVersion>()
        var previous: SemanticVersion? = null
        document.headings.forEachIndexed { index, heading ->
            if (heading.isUnreleased && index > 0) {
                problems += ChangelogProblem("""The "Unreleased" section must come first.""", heading.line)
            }
            val parsed = releaseOf(heading, problems)
            if (parsed != null && !seen.add(parsed)) {
                problems += ChangelogProblem("Version ${heading.version} is listed more than once.", heading.line)
            }
            val above = previous
            if (parsed != null && above != null && parsed > above) {
                val message = "Version ${heading.version} is listed below the lower version $above; " +
                    "list the newest first."
                problems += ChangelogProblem(message, heading.line)
            }
            if (parsed != null) previous = parsed
        }
        return problems + documented(document, version)
    }

    private fun releaseOf(heading: ChangelogHeading, problems: MutableList<ChangelogProblem>): SemanticVersion? {
        if (heading.isUnreleased) return null
        val version = heading.version
        if (version == null) {
            problems += ChangelogProblem(""""## ${heading.text}" is no release heading. $HEADING_FORMS""", heading.line)
            return null
        }
        val parsed = SemanticVersion.parse(version)
        if (parsed == null) {
            problems += ChangelogProblem(""""$version" is no semantic version such as 1.2.3.""", heading.line)
        }
        val date = heading.date
        if (date != null && !isDate(date)) {
            problems += ChangelogProblem(""""$date" is no date of the form YYYY-MM-DD.""", heading.line)
        }
        return parsed
    }

    private fun documented(document: ChangelogDocument, version: String?): List<ChangelogProblem> {
        if (version == null) return emptyList()
        val heading = document.find(version)
            ?: return listOf(ChangelogProblem("Version $version has no section. Add one before releasing it."))
        if (document.body(heading).isNotEmpty()) return emptyList()
        return listOf(ChangelogProblem("The section of $version is empty.", heading.line))
    }

    private fun isDate(text: String): Boolean = try {
        LocalDate.parse(text)
        true
    } catch (_: DateTimeParseException) {
        false
    }
}
