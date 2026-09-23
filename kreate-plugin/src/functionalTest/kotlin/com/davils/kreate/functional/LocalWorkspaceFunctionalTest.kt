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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for orchestrating a local publish across several repositories.
 *
 * ### Why the repositories here are scripts rather than Gradle builds
 *
 * `kreateLocalPublishAll` has exactly one job: run each repository's own wrapper, in dependency
 * order, in the right directory, with the right arguments, and stop at the first failure. Every
 * one of those is a property of the command line it issues.
 *
 * So each "repository" here is a `gradlew` script that records how it was invoked. That makes the
 * assertions exact — the real order, the real working directory, the real arguments — where
 * driving genuine Gradle builds would assert the same things far more slowly and through a much
 * larger surface. That `kreateLocalPublish` itself works is established by
 * `LocalPublishFunctionalTest`; there is nothing gained by proving it again once per library.
 *
 * Disabled on Windows: the point of the fixture is a wrapper script, and maintaining a second
 * batch-file implementation of it would be upkeep on test scaffolding rather than on the feature.
 */
@DisabledOnOs(OS.WINDOWS, disabledReason = "The fixture's wrapper is a POSIX shell script.")
@DisplayName("kreateLocalPublishAll")
class LocalWorkspaceFunctionalTest {

    @TempDir
    lateinit var workspaceDirectory: File

    private val invocationLog: File get() = File(workspaceDirectory, "invocations.log")

    /**
     * Writes a repository whose wrapper records its invocation.
     *
     * @param name The library's name and directory.
     * @param succeeds Whether the wrapper exits zero.
     */
    private fun repository(name: String, succeeds: Boolean = true) {
        val directory = File(workspaceDirectory, name).apply { mkdirs() }
        val wrapper = File(directory, "gradlew")

        wrapper.writeText(
            """
            #!/bin/sh
            echo "$name |${'$'}(pwd)| ${'$'}*" >> "${invocationLog.absolutePath}"
            ${if (succeeds) "exit 0" else """echo "boom" >&2; exit 1"""}
            """.trimIndent() + "\n"
        )
        wrapper.setExecutable(true)
    }

    /**
     * Writes the orchestrating repository that declares the workspace.
     *
     * @param declarations The body of the `workspace { }` block.
     * @return The fixture, ready to build.
     */
    private fun orchestrator(declarations: String): KreateBuildFixture {
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

    /**
     * The chain the Davils graph actually has: each library depends on the one before it.
     */
    private val chain = """
        library("alpha")
        library("beta")  { dependsOn("alpha") }
        library("gamma") { dependsOn("beta") }
    """.trimIndent()

    private fun invocations(): List<String> =
        invocationLog.takeIf { it.isFile }?.readLines().orEmpty()

    @Test
    @DisplayName("runs every repository in dependency order")
    fun publishesInOrder() {
        listOf("alpha", "beta", "gamma").forEach { repository(it) }
        val tooling = orchestrator(chain)

        tooling.build("kreateLocalPublishAll")

        // The order is the assertion. `beta` published before `alpha` would still succeed — it
        // would just be built against the released `alpha`, which is the failure mode that never
        // announces itself.
        invocations().map { it.substringBefore(" |") } shouldBe listOf("alpha", "beta", "gamma")
    }

    @Test
    @DisplayName("runs each wrapper in its own repository, with the publish flag set")
    fun invokesCorrectly() {
        listOf("alpha", "beta", "gamma").forEach { repository(it) }
        val tooling = orchestrator(chain)

        tooling.build("kreateLocalPublishAll")

        val alpha = invocations().first()
        alpha shouldContain File(workspaceDirectory, "alpha").absolutePath
        alpha shouldContain "kreateLocalPublish"

        // Without this the sub-build would publish a release version into the local repository,
        // because it has no other way to know it was asked for a local publish.
        alpha shouldContain "-Pdavils.local.publish=true"
    }

    @Test
    @DisplayName("--from runs the named repository and everything downstream of it")
    fun publishesFrom() {
        listOf("alpha", "beta", "gamma").forEach { repository(it) }
        val tooling = orchestrator(chain)

        tooling.build("kreateLocalPublishAll", "--from", "beta")

        // `alpha` is upstream of the change: republishing it would cost time and overwrite a
        // snapshot someone may be relying on.
        invocations().map { it.substringBefore(" |") } shouldBe listOf("beta", "gamma")
    }

    @Test
    @DisplayName("--only runs exactly what is named")
    fun publishesOnly() {
        listOf("alpha", "beta", "gamma").forEach { repository(it) }
        val tooling = orchestrator(chain)

        tooling.build("kreateLocalPublishAll", "--only", "beta")

        invocations().map { it.substringBefore(" |") } shouldBe listOf("beta")
    }

    @Test
    @DisplayName("honours a per-repository task override")
    fun taskOverride() {
        listOf("alpha").forEach { repository(it) }
        val tooling = orchestrator(
            """library("alpha") { tasks("kreateLocalPublish", ":nested:publishToMavenLocal") }"""
        )

        tooling.build("kreateLocalPublishAll")

        // This is how `novy-gradle` and `mica-openapi-gradle` are installed alongside their
        // library: they are ordinary artefacts that the library's own task cannot reach.
        invocations().single() shouldContain ":nested:publishToMavenLocal"
    }

    @Test
    @DisplayName("stops at the first failure and names what did not run")
    fun stopsOnFailure() {
        repository("alpha")
        repository("beta", succeeds = false)
        repository("gamma")
        val tooling = orchestrator(chain)

        val result = tooling.buildAndFail("kreateLocalPublishAll")

        result.output shouldContain "Publishing 'beta' failed"
        result.output shouldContain "Not run:    gamma"

        // Half a workspace is a real state to be left in, and the message says so rather than
        // pretending the run was atomic.
        result.output shouldContain "half updated"
        invocations().map { it.substringBefore(" |") } shouldBe listOf("alpha", "beta")
    }

    @Test
    @DisplayName("refuses a cycle while configuring, before any repository is touched")
    fun refusesCycle() {
        listOf("alpha", "beta").forEach { repository(it) }
        val tooling = orchestrator(
            """
            library("alpha") { dependsOn("beta") }
            library("beta")  { dependsOn("alpha") }
            """.trimIndent()
        )

        val result = tooling.buildAndFail("kreateLocalPublishAll")

        result.output shouldContain "dependency cycle"
        result.output shouldContain "alpha -> beta -> alpha"
        invocations() shouldBe emptyList()
    }

    @Test
    @DisplayName("refuses an edge pointing at a library the workspace does not declare")
    fun refusesUndeclaredEdge() {
        repository("beta")
        val tooling = orchestrator("""library("beta") { dependsOn("alpha") }""")

        val result = tooling.buildAndFail("kreateLocalPublishAll")

        result.output shouldContain "beta depends on 'alpha'"
        invocations() shouldBe emptyList()
    }

    @Test
    @DisplayName("says so when a declared repository has no wrapper")
    fun missingWrapper() {
        File(workspaceDirectory, "alpha").mkdirs()
        val tooling = orchestrator("""library("alpha")""")

        val result = tooling.buildAndFail("kreateLocalPublishAll")

        result.output shouldContain "No Gradle wrapper"
        result.output shouldContain "alpha"
    }

    @Test
    @DisplayName("is not registered in a repository that declares no workspace")
    fun notRegisteredWithoutAWorkspace() {
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

        // A library cannot orchestrate a workspace whose shape it does not know, and a task that
        // would always fail is worse than no task.
        result.output shouldContain "kreateLocalPublish"
        (result.output.contains("kreateLocalPublishAll")) shouldBe false
    }
}
