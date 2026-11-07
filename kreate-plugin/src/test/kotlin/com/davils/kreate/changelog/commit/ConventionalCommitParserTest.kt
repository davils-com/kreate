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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun parsed(message: String): ConventionalCommit =
    ConventionalCommitParser.parse(GitCommit("abc1234def", message))

class ConventionalCommitParserTest : FunSpec({
    context("a commit") {
        test("names type, scope and description") {
            parsed("feat(plugin): add the gate") shouldBe
                ConventionalCommit("abc1234def", "feat", "add the gate", scope = "plugin")
        }

        test("is breaking with a bang") {
            parsed("feat(plugin)!: provision Trivy").breaking shouldBe true
        }

        test("is breaking with a footer, which keeps its note") {
            val message = """
                refactor: rename

                Body.

                BREAKING CHANGE: Trivy is pinned
                now.
                Refs: #12
            """.trimIndent()

            val commit = parsed(message)

            commit.breaking shouldBe true
            commit.breakingNotes shouldBe listOf("Trivy is pinned\nnow.")
        }

        test("without the convention keeps its subject") {
            parsed("Update README") shouldBe ConventionalCommit("abc1234def", null, "Update README")
        }

        test("that reverts another names it") {
            val commit = parsed("Revert \"feat: add x\"\n\nThis reverts commit 0123456789abcdef.")

            commit.type shouldBe "revert"
            commit.description shouldBe "Revert \"add x\""
            commit.revertedHashes shouldBe listOf("0123456789abcdef")
        }
    }
})
