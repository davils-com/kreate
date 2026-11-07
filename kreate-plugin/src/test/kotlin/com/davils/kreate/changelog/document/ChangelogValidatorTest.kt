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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

private val VALID: String = """
    # Changelog

    ## Unreleased

    - Something.

    ## [1.1.0] - 2026-10-04

    ### Breaking changes

    - A change.

    ```markdown
    ## not a heading
    ```

    ## 1.0.0

    - First.
""".trimIndent()

private fun problemsOf(text: String, version: String? = null): List<String> {
    val problems = ChangelogValidator.validate(ChangelogDocument.parse(text), version)
    return problems.map { problem -> problem.message }
}

class ChangelogValidatorTest : FunSpec({
    context("a changelog") {
        test("in the Keep a Changelog form is valid, also with fenced headings") {
            problemsOf(VALID, "1.1.0").shouldBeEmpty()
        }

        test("of Kreate itself is valid") {
            problemsOf(File("../CHANGELOG.md").readText()).shouldBeEmpty()
        }

        test("must list the newest release first, each once") {
            val swapped = listOf("## 1.0.0", "- a", "## 1.1.0", "- b", "## 1.0.0", "- c").joinToString("\n\n")

            problemsOf(swapped) shouldBe listOf(
                "Version 1.1.0 is listed below the lower version 1.0.0; list the newest first.",
                "Version 1.0.0 is listed more than once."
            )
        }

        test("names the line of a heading that is no release") {
            val problems = ChangelogValidator.validate(ChangelogDocument.parse("# C\n\n## Notes\n"), null)

            problems.single().line shouldBe 3
        }

        test("rejects a version and a date in the wrong form") {
            problemsOf("## 1.0 - 2026-13-40\n\n- a\n") shouldBe listOf(
                "\"1.0\" is no semantic version such as 1.2.3.",
                "\"2026-13-40\" is no date of the form YYYY-MM-DD."
            )
            val german = problemsOf("## 1.0.0 - 07.11.2026\n\n- a\n")

            german shouldBe listOf(""""07.11.2026" is no date of the form YYYY-MM-DD.""")
        }

        test("must document the version and not leave it empty") {
            problemsOf(VALID, "2.0.0").single() shouldContain "Version 2.0.0 has no section"
            problemsOf("## 2.0.0\n\n## 1.0.0\n\n- a\n", "2.0.0") shouldBe listOf("The section of 2.0.0 is empty.")
        }
    }

    context("the writer") {
        test("puts a new release below Unreleased and above the last release") {
            val updated = ChangelogWriter.insert(VALID, "1.2.0", "## 1.2.0 - 2026-11-07\n\n### Added\n\n- New.\n")

            val headings = ChangelogDocument.parse(updated).headings.map { heading -> heading.version }
            headings shouldBe listOf(null, "1.2.0", "1.1.0", "1.0.0")
            problemsOf(updated, "1.2.0").shouldBeEmpty()
        }

        test("starts a changelog that does not exist yet") {
            val started = ChangelogWriter.insert("", "1.0.0", "## 1.0.0\n\n- First.\n")

            started shouldBe "# Changelog\n\n## 1.0.0\n\n- First.\n"
        }

        test("refuses a release that is already there") {
            shouldThrow<IllegalArgumentException> { ChangelogWriter.insert(VALID, "1.1.0", "## 1.1.0\n") }
        }
    }
})
