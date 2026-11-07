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

private const val ISOLATED_PROJECTS: String = "-Dorg.gradle.unsafe.isolated-projects=true"

private const val PROCESS_TIMEOUT_SECONDS: Long = 60

private const val PROJECT_A_DEPENDENCIES: String = """dependencies { implementation(project(":b")) }"""

private const val ROOT_DEPENDENCIES: String =
    """dependencies { implementation("org.apache.commons:commons-lang3:3.14.0") }"""

private val PROJECT_B_DEPENDENCIES: String = """
    dependencies { implementation("org.apache.commons:commons-lang3:3.18.0") }
""".trimIndent()

private fun run(directory: File, vararg command: String): String {
    val builder = ProcessBuilder(command.toList())
    builder.directory(directory)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    check(process.exitValue() == 0) { "${command.joinToString(" ")} failed: $output" }
    return output
}

private fun gitIn(directory: File, vararg arguments: String): String {
    val identity = arrayOf("-c", "user.name=Kreate", "-c", "user.email=kreate@example.com")
    val unsigned = arrayOf("-c", "commit.gpgsign=false")
    return run(directory, "git", *identity, *unsigned, *arguments)
}

class ToolsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(kreateBlock: String, extra: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild("${KreateBuildFixture.platformBlock}\n\n$kreateBlock", extra = extra)
        return fixture
    }

    fun newWorkspace(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        val settings = fixture.file("settings.gradle.kts").readText()
        fixture.write("settings.gradle.kts", settings + "\n" + """include("a", "b")""" + "\n")
        val assurance = """
            assurance {
                enabled = true
                workspace {
                    enabled = true
                    members = setOf(":", ":a", ":b")
                    forbid(":a", ":b")
                }
            }
        """.trimIndent()
        fixture.writeBuild("${KreateBuildFixture.platformBlock}\n\n$assurance")
        val script = fixture.file("build.gradle.kts").readText()
        fixture.write("a/build.gradle.kts", script + "\n" + PROJECT_A_DEPENDENCIES + "\n")
        fixture.write("b/build.gradle.kts", script + "\n" + PROJECT_B_DEPENDENCIES + "\n")
        fixture.write("build.gradle.kts", script + "\n" + ROOT_DEPENDENCIES + "\n")
        fixture.write("a/src/main/kotlin/A.kt", "class A")
        fixture.write("b/src/main/kotlin/B.kt", "class B")
        return fixture
    }

    context("the workspace") {
        test("finds misaligned versions and forbidden dependencies across projects, with Isolated Projects") {
            val fixture = newWorkspace()

            val result = fixture.build("kreateWorkspaceCheck", ISOLATED_PROJECTS)

            result.output shouldContain "Checked 3 project(s) of the workspace"
            val document = fixture.file("build/kreate/assurance/documents/workspace.json").readText()
            document shouldContain "WORKSPACE_VERSION_MISMATCH"
            document shouldContain "org.apache.commons:commons-lang3 resolves as 3.14.0 (:); 3.18.0 (:a, :b)"
            document shouldContain "FORBIDDEN_DEPENDENCY"
        }

        test("draws how the projects depend on each other") {
            val fixture = newWorkspace()

            val result = fixture.build("kreateWorkspaceGraph")

            result.output shouldContain ":a:\n+--- :b"
        }

        test("lists the changed project and its dependents") {
            val fixture = newWorkspace()
            val root = fixture.file(".")
            fixture.write(".gitignore", "build/\n.gradle/\n.kotlin/\n")
            gitIn(root, "init", "-q")
            gitIn(root, "add", "-A")
            gitIn(root, "commit", "-q", "-m", "start")
            fixture.write("b/src/main/kotlin/B.kt", "class B { fun changed() = 1 }")

            val result = fixture.build(":kreateWorkspaceAffected", "--base=HEAD")

            fixture.file("build/kreate/workspace/affected.txt").readText() shouldBe ":a\n:b\n"
            result.output shouldContain "2 project(s) affected since HEAD"
        }

        test("draws the dependencies of one project") {
            val fixture = newWorkspace()

            val result = fixture.build(":a:kreateDependencyGraph")

            result.output shouldContain "+--- :b\n|   +--- org.apache.commons:commons-lang3:3.18.0"
            fixture.build(":a:kreateDependencyGraph", "--format=mermaid")
            fixture.file("a/build/kreate/dependencies/graph.mmd").readText() shouldContain "graph LR"
        }
    }

    context("the pre-commit hook") {
        test("is installed into the Git directory, never over a foreign one, and removed again") {
            val fixture = newBuild("project { gitHooks { enabled = true } }")
            val root = fixture.file(".")
            gitIn(root, "init", "-q")

            fixture.build("kreateGitHookInstall")

            val hook = fixture.file(".git/hooks/pre-commit")
            hook.readText() shouldContain "kreateGitHookRun"
            fixture.build("kreateGitHookUninstall")
            hook.exists() shouldBe false
            hook.writeText("#!/bin/sh\necho mine\n")
            fixture.buildAndFail("kreateGitHookInstall").output shouldContain "Kreate does not replace it"
        }

        test("runs the checks the project has") {
            val fixture = newBuild(
                """
                project {
                    gitHooks { enabled = true }
                    changelog { enabled = true }
                }
                """.trimIndent()
            )
            fixture.write("CHANGELOG.md", "# Changelog\n\n## 1.0.0\n\n- First.\n")

            val result = fixture.build("kreateGitHookRun")

            result.output shouldContain ":kreateChangelogCheck"
        }
    }

    context("the doctor") {
        test("examines the machine and passes offline") {
            val fixture = newBuild("")

            val result = fixture.build("kreateDoctor", "--offline")

            result.output shouldContain "OK    Java"
            result.output shouldContain "Offline"
        }
    }

    context("the assurance tools") {
        val assurance = """
            assurance {
                enabled = true
                reportOnly = true
                advisories { enabled = true }
            }
        """.trimIndent()

        test("explain a rule, an advisory and refuse an unknown rule") {
            val fixture = newBuild(assurance)

            val explained = fixture.build("kreateExplain", "--rule=fresh_release")

            explained.output shouldContain "FRESH_RELEASE (CRITICAL, trust/age)"
            fixture.build("kreateExplain", "--rule=GHSA-abcd-efgh-ijkl").output shouldContain
                "https://osv.dev/vulnerability/GHSA-ABCD-EFGH-IJKL"
            fixture.buildAndFail("kreateExplain", "--rule=FRESH").output shouldContain "Did you mean"
            fixture.build("kreateExplain").output shouldContain "TYPOSQUAT_GROUP"
        }

        test("show the configuration and lint it into the gate") {
            val fixture = newBuild(assurance)

            fixture.build("kreateAssuranceConfig").output shouldContain "* reportOnly"
            fixture.build("kreateAssurance")

            fixture.file("build/kreate/assurance/documents/config.json").readText() shouldContain
                "CONFIG_GATE_NOT_FAILING"
        }
    }

    context("formatting") {
        test("rewrites the sources to follow the formatting rules") {
            val fixture = newBuild("project { detekt { enabled = true\nformatting = true } }")
            fixture.write("detekt.yaml", "")
            fixture.writeKotlin("Sample.kt", "class Sample{\n    fun answer( ) : Int   = 42\n}\n")

            fixture.build("kreateFormatApply")

            val text = fixture.file("src/main/kotlin/Sample.kt").readText()
            text shouldContain "class Sample {"
            text shouldNotContain "answer( )"
        }
    }
})
