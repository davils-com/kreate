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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun commit(
    hash: String,
    type: String?,
    description: String,
    breaking: Boolean = false,
    reverts: String? = null
): ConventionalCommit =
    ConventionalCommit(hash, type, description, breaking = breaking, revertedHashes = listOfNotNull(reverts))

class ChangelogBuilderTest : FunSpec({
    context("the changes") {
        test("are grouped by section, breaking first, without what is hidden") {
            val changes = ChangelogBuilder.build(
                listOf(
                    commit("a1", "feat", "add a"),
                    commit("b2", "fix", "fix b"),
                    commit("c3", "chore", "tidy"),
                    commit("d4", "refactor", "rename", breaking = true)
                )
            )

            changes.sections.keys.toList() shouldBe
                listOf(ChangelogSection.BREAKING, ChangelogSection.ADDED, ChangelogSection.FIXED)
            changes.bump shouldBe VersionBump.MAJOR
        }

        test("suggest a minor release for a feature and a patch otherwise") {
            ChangelogBuilder.build(listOf(commit("a1", "feat", "add"))).bump shouldBe VersionBump.MINOR
            ChangelogBuilder.build(listOf(commit("a1", "fix", "fix"))).bump shouldBe VersionBump.PATCH
            ChangelogBuilder.build(listOf(commit("a1", "docs", "doc"))).bump shouldBe null
        }

        test("leave out a commit and its revert") {
            val changes = ChangelogBuilder.build(
                listOf(commit("feed1234", "feat", "add a"), commit("b2", "revert", "Revert", reverts = "feed123"))
            )

            changes.sections shouldBe emptyMap()
        }

        test("list a change once") {
            val changes = ChangelogBuilder.build(listOf(commit("a1", "fix", "fix b"), commit("b2", "fix", "fix b")))

            changes.sections.getValue(ChangelogSection.FIXED).size shouldBe 1
        }
    }

    context("the section") {
        test("is rendered with a date, scopes, short hashes and notes") {
            val entry = ChangelogEntry("0123456789", "plugin", "pin Trivy", listOf("Set useInstalled.\nOr not."))
            val changes = ChangelogChanges(mapOf(ChangelogSection.BREAKING to listOf(entry)), VersionBump.MAJOR)

            val expected = """
                ## 2.0.0 - 2026-11-07

                ### Breaking changes

                - **plugin:** pin Trivy (0123456)
                  Set useInstalled.
                  Or not.

            """.trimIndent()
            ChangelogRenderer.render("2.0.0", "2026-11-07", changes) shouldBe expected
        }

        test("says so when nothing notable changed") {
            ChangelogRenderer.render("1.0.1", null, ChangelogChanges(emptyMap(), null)) shouldBe
                "## 1.0.1\n\nNo notable changes.\n"
        }
    }
})
