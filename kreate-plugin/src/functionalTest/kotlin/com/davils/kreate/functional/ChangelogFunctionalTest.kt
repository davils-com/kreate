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
package com.davils.kreate.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import java.util.concurrent.TimeUnit

private const val GIT_TIMEOUT_SECONDS: Long = 60

private fun git(directory: File, vararg arguments: String): String {
    val identity = listOf("-c", "user.name=Kreate", "-c", "user.email=kreate@example.com")
    val unsigned = listOf("-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false")
    val builder = ProcessBuilder(listOf("git") + identity + unsigned + arguments)
    builder.directory(directory)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    check(process.exitValue() == 0) { "git ${arguments.joinToString(" ")} failed: $output" }
    return output
}

private fun KreateBuildFixture.commit(message: String) {
    git(rootDirectoryOf(this), "add", "-A")
    git(rootDirectoryOf(this), "commit", "--allow-empty", "-q", "-m", message)
}

private fun rootDirectoryOf(fixture: KreateBuildFixture): File = fixture.file(".")

class ChangelogFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun KreateBuildFixture.writeSource(body: String) {
        writeKotlin("com/example/Sample.kt", "package com.example\n\n$body\n")
    }

    fun newRepository(projectBlock: String): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            project {
                $projectBlock
            }
            """.trimIndent()
        )
        fixture.write(".gitignore", "build/\n.gradle/\n.kotlin/\n")
        git(rootDirectoryOf(fixture), "init", "-q", "-b", "main")
        return fixture
    }

    context("the changelog") {
        test("is generated from the conventional commits since the last release and written on request") {
            val fixture = newRepository("changelog { enabled = true }")
            fixture.write("CHANGELOG.md", "# Changelog\n\n## 1.0.0\n\n- First.\n")
            fixture.commit("feat: start")
            git(rootDirectoryOf(fixture), "tag", "1.0.0")
            fixture.commit("feat(api): add the greeting")
            fixture.commit("fix: spell the greeting right")
            fixture.commit("chore: tidy up")

            val result = fixture.build("kreateChangelogGenerate", "--write")

            result.output shouldContain "## 1.1.0 - "
            val changelog = fixture.file("CHANGELOG.md").readText()
            changelog shouldContain "### Added\n\n- **api:** add the greeting"
            changelog shouldContain "### Fixed\n\n- spell the greeting right"
            changelog shouldNotContain "tidy up"
        }

        test("suggests a major release after a breaking commit") {
            val fixture = newRepository("changelog { enabled = true }")
            fixture.commit("feat: start")
            git(rootDirectoryOf(fixture), "tag", "1.0.0")
            fixture.commit("feat!: replace the greeting")

            fixture.build("kreateChangelogGenerate").output shouldContain "## 2.0.0 - "
        }

        test("check fails when the project version has no section, and check runs it") {
            val fixture = newRepository("changelog { enabled = true }")
            fixture.write("CHANGELOG.md", "# Changelog\n\n## 0.9.0\n\n- Old.\n")
            fixture.withEnvironment("VERSION", "1.0.0")

            val result = fixture.buildAndFail("check")

            result.output shouldContain "CHANGELOG.md has 1 problem(s)"
            result.output shouldContain "Version 1.0.0 has no section"
        }

        test("notes are the section of a release") {
            val fixture = newRepository("changelog { enabled = true }")
            val sections = listOf("# Changelog", "## 1.0.0", "### Added", "- First.", "## 0.9.0", "- Old.")
            fixture.write("CHANGELOG.md", sections.joinToString("\n\n"))
            fixture.withEnvironment("VERSION", "1.0.0")

            fixture.build("kreateChangelogNotes")

            fixture.file("build/kreate/changelog/notes.md").readText() shouldBe "### Added\n\n- First.\n"
        }
    }

    context("the semantic versioning check") {
        val semverBlock = """
            apiValidation {
                enabled = true
                semver { enabled = true }
            }
        """.trimIndent()

        fun released(): KreateBuildFixture {
            val fixture = newRepository(semverBlock)
            fixture.writeSource("class Sample {\n    fun greet(): String = \"hello\"\n    fun wave(): Int = 1\n}")
            fixture.withEnvironment("VERSION", "1.0.0")
            fixture.build("kreateApiDump")
            fixture.commit("feat: greet")
            git(rootDirectoryOf(fixture), "tag", "1.0.0")
            fixture.writeSource("class Sample {\n    fun greet(): String = \"hello\"\n}")
            fixture.build("kreateApiDump")
            return fixture
        }

        test("fails when a breaking change comes without a major release") {
            val fixture = released()
            fixture.commit("refactor!: drop the wave")
            fixture.withEnvironment("VERSION", "1.1.0")

            val result = fixture.buildAndFail("kreateApiSemverCheck")

            result.output shouldContain "breaking: com/example/Sample.wave ()I was removed"
            result.output shouldContain "needs version 2.0.0 or higher, but the version is 1.1.0"
        }

        test("fails when the breaking change is not announced by a commit") {
            val fixture = released()
            fixture.commit("refactor: drop the wave")
            fixture.withEnvironment("VERSION", "2.0.0")

            val result = fixture.buildAndFail("kreateApiSemverCheck")

            result.output shouldContain "no commit since 1.0.0 is marked as breaking"
        }

        test("passes a major release with an announced breaking change") {
            val fixture = released()
            fixture.commit("refactor!: drop the wave")
            fixture.withEnvironment("VERSION", "2.0.0")

            fixture.build("kreateApiSemverCheck").output shouldContain "they need a major release, the version is 2.0.0"
        }

        test("is skipped before the first release") {
            val fixture = newRepository(semverBlock)
            fixture.writeSource("class Sample")
            fixture.build("kreateApiDump")
            fixture.commit("feat: start")

            fixture.build("kreateApiSemverCheck").output shouldContain "skipped: there is no release tag"
        }
    }
})
