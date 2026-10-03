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
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.io.path.createTempDirectory

class LocalWorkspaceFunctionalTest : FunSpec({

    val workspace = tempdir()

    val chain = """
        library("alpha")
        library("beta")  { dependsOn("alpha") }
        library("gamma") { dependsOn("beta") }
    """.trimIndent()

    fun newWorkspaceDirectory(): File = createTempDirectory(workspace.toPath(), "workspace").toFile()

    fun invocationLog(workspaceDirectory: File): File = File(workspaceDirectory, "invocations.log")

    fun exitLine(succeeds: Boolean): String {
        if (succeeds) return "exit 0"
        return """echo "boom" >&2; exit 1"""
    }

    fun repository(workspaceDirectory: File, name: String, succeeds: Boolean = true) {
        val directory = File(workspaceDirectory, name).apply { mkdirs() }
        val wrapper = File(directory, "gradlew")

        wrapper.writeText(
            """
            #!/bin/sh
            echo "$name |${'$'}(pwd)| ${'$'}*" >> "${invocationLog(workspaceDirectory).absolutePath}"
            ${exitLine(succeeds)}
            """.trimIndent() + "\n"
        )
        wrapper.setExecutable(true)
    }

    fun repositories(workspaceDirectory: File, vararg names: String) {
        names.forEach { name -> repository(workspaceDirectory, name) }
    }

    fun orchestrator(workspaceDirectory: File, declarations: String): KreateBuildFixture {
        val directory = File(workspaceDirectory, "tooling").apply { mkdirs() }
        val fixture = KreateBuildFixture(directory)
        fixture.sharedLocations(
            File(workspaceDirectory, "state"),
            File(workspaceDirectory, "maven-local")
        )

        fixture.writeSettings("tooling")
        fixture.writeBuild(
            kreateBlock = """
                project {
                    name = "tooling"
                    version { property = "tooling.version" }
                }
                ${KreateBuildFixture.platformBlock}
                local {
                    workspace {
                        root = file("..")

                        $declarations
                    }
                }
            """.trimIndent()
        )
        fixture.write("gradle.properties", "tooling.version=1.0.0")
        fixture.writeKotlin("Tooling.kt", "class Tooling")
        return fixture
    }

    fun invocations(workspaceDirectory: File): List<String> {
        val log = invocationLog(workspaceDirectory)
        if (!log.isFile) return emptyList()
        return log.readLines()
    }

    fun invokedRepositories(workspaceDirectory: File): List<String> =
        invocations(workspaceDirectory).map { it.substringBefore(" |") }

    context("kreateLocalPublishAll").config(enabledOrReasonIf = requiresPosixShell) {
        test("runs every repository in dependency order") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha", "beta", "gamma")
            val tooling = orchestrator(workspaceDirectory, chain)

            tooling.build("kreateLocalPublishAll")

            invokedRepositories(workspaceDirectory) shouldBe listOf("alpha", "beta", "gamma")
        }

        test("runs each wrapper in its own repository, with the publish flag set") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha", "beta", "gamma")
            val tooling = orchestrator(workspaceDirectory, chain)

            tooling.build("kreateLocalPublishAll")

            val alpha = invocations(workspaceDirectory).first()
            alpha shouldContain File(workspaceDirectory, "alpha").absolutePath
            alpha shouldContain "kreateLocalPublish"
            alpha shouldContain "-Pkreate.local.publish=true"
        }

        test("--from runs the named repository and everything downstream of it") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha", "beta", "gamma")
            val tooling = orchestrator(workspaceDirectory, chain)

            tooling.build("kreateLocalPublishAll", "--from", "beta")

            invokedRepositories(workspaceDirectory) shouldBe listOf("beta", "gamma")
        }

        test("--only runs exactly what is named") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha", "beta", "gamma")
            val tooling = orchestrator(workspaceDirectory, chain)

            tooling.build("kreateLocalPublishAll", "--only", "beta")

            invokedRepositories(workspaceDirectory) shouldBe listOf("beta")
        }

        test("honours a per-repository task override") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha")
            val tooling = orchestrator(
                workspaceDirectory,
                """library("alpha") { tasks("kreateLocalPublish", ":nested:publishToMavenLocal") }"""
            )

            tooling.build("kreateLocalPublishAll")

            invocations(workspaceDirectory).single() shouldContain ":nested:publishToMavenLocal"
        }

        test("stops at the first failure and names what did not run") {
            val workspaceDirectory = newWorkspaceDirectory()
            repository(workspaceDirectory, "alpha")
            repository(workspaceDirectory, "beta", succeeds = false)
            repository(workspaceDirectory, "gamma")
            val tooling = orchestrator(workspaceDirectory, chain)

            val result = tooling.buildAndFail("kreateLocalPublishAll")

            result.output shouldContain "Publishing 'beta' failed"
            result.output shouldContain "Not run:    gamma"
            result.output shouldContain "half updated"
            invokedRepositories(workspaceDirectory) shouldBe listOf("alpha", "beta")
        }

        test("refuses a cycle while configuring, before any repository is touched") {
            val workspaceDirectory = newWorkspaceDirectory()
            repositories(workspaceDirectory, "alpha", "beta")
            val tooling = orchestrator(
                workspaceDirectory,
                """
                library("alpha") { dependsOn("beta") }
                library("beta")  { dependsOn("alpha") }
                """.trimIndent()
            )

            val result = tooling.buildAndFail("kreateLocalPublishAll")

            result.output shouldContain "dependency cycle"
            result.output shouldContain "alpha -> beta -> alpha"
            invocations(workspaceDirectory) shouldBe emptyList()
        }

        test("refuses an edge pointing at a library the workspace does not declare") {
            val workspaceDirectory = newWorkspaceDirectory()
            repository(workspaceDirectory, "beta")
            val tooling = orchestrator(workspaceDirectory, """library("beta") { dependsOn("alpha") }""")

            val result = tooling.buildAndFail("kreateLocalPublishAll")

            result.output shouldContain "beta depends on 'alpha'"
            invocations(workspaceDirectory) shouldBe emptyList()
        }

        test("says so when a declared repository has no wrapper") {
            val workspaceDirectory = newWorkspaceDirectory()
            File(workspaceDirectory, "alpha").mkdirs()
            val tooling = orchestrator(workspaceDirectory, """library("alpha")""")

            val result = tooling.buildAndFail("kreateLocalPublishAll")

            result.output shouldContain "No Gradle wrapper"
            result.output shouldContain "alpha"
        }

        test("is not registered in a repository that declares no workspace") {
            val workspaceDirectory = newWorkspaceDirectory()
            val plain = KreateBuildFixture(File(workspaceDirectory, "plain").apply { mkdirs() })
            plain.writeSettings("plain")
            plain.writeBuild(
                kreateBlock = """
                    project {
                        name = "plain"
                        version { property = "plain.version" }
                    }
                    ${KreateBuildFixture.platformBlock}
                """.trimIndent()
            )
            plain.write("gradle.properties", "plain.version=1.0.0")
            plain.writeKotlin("Plain.kt", "class Plain")

            val result = plain.build("tasks", "--group", "kreate local")

            result.output shouldContain "kreateLocalPublish"
            result.output shouldNotContain "kreateLocalPublishAll"
        }
    }
})
