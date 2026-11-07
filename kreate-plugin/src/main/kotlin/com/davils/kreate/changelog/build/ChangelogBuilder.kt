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

import com.davils.kreate.changelog.commit.ConventionalCommit
import com.davils.kreate.vcs.VersionBump

internal object ChangelogBuilder {
    fun build(commits: List<ConventionalCommit>): ChangelogChanges {
        val effective = withoutReverted(commits)
        val listed = effective.filter { commit -> sectionOf(commit) != ChangelogSection.HIDDEN }
        val unique = listed.distinctBy { commit ->
            listOf(commit.breaking, commit.type, commit.scope, commit.description)
        }
        val grouped = unique.groupBy { commit -> sectionOf(commit) }
        val sections = ChangelogSection.entries.mapNotNull { section ->
            grouped[section]?.let { entries -> section to entries.map(::entryOf) }
        }
        val bump = unique.maxOfOrNull { commit -> bumpOf(commit) }
        return ChangelogChanges(sections.toMap(), bump)
    }

    private fun sectionOf(commit: ConventionalCommit): ChangelogSection {
        if (commit.breaking) return ChangelogSection.BREAKING
        return ChangelogSection.sectionOf(commit.type)
    }

    private fun entryOf(commit: ConventionalCommit): ChangelogEntry =
        ChangelogEntry(commit.hash, commit.scope, commit.description, commit.breakingNotes)

    private fun bumpOf(commit: ConventionalCommit): VersionBump {
        val section = sectionOf(commit)
        if (section == ChangelogSection.BREAKING) return VersionBump.MAJOR
        if (section == ChangelogSection.ADDED) return VersionBump.MINOR
        return VersionBump.PATCH
    }

    private fun withoutReverted(commits: List<ConventionalCommit>): List<ConventionalCommit> {
        val dropped = mutableSetOf<String>()
        commits.forEach { revert ->
            revert.revertedHashes.forEach { reverted ->
                val original = commits.firstOrNull { commit -> commit.hash.lowercase().startsWith(reverted) }
                val isOpen = revert.hash !in dropped
                val pairs = original != null && original.hash !in dropped && isOpen
                if (pairs) dropped += listOf(original.hash, revert.hash)
            }
        }
        return commits.filterNot { commit -> commit.hash in dropped }
    }
}
