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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the defaults that are named after what the Kotlin JVM plugin registers and therefore
 * match nothing under the Kotlin Multiplatform plugin.
 *
 * Each one fails the same way: the build stays green and the thing quietly stops happening. Nothing
 * here asserts that a task succeeded - that was already true while it was broken. Every assertion is
 * about a task having actually done its work.
 */
@DisplayName("Kotlin Multiplatform")
class MultiplatformFunctionalTest {
    private companion object {
        /**
         * A Java version deliberately different from the one running the test.
         *
         * The pin is only worth asserting against a version the build would not have arrived at on
         * its own - checking that a build on JDK 21 targets 21 passes just as well when nothing was
         * pinned at all.
         */
        val PINNED_JAVA: Int = if (KreateBuildFixture.javaVersion == 17) 21 else 17
    }

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()

        // One file per source set, because a task with nothing to read reports NO-SOURCE and would
        // let an assertion that it ran pass while it read nothing - the failure this whole class is
        // about. The bodies do real work for the same reason: Detekt's default configuration
        // objects to a function that only returns a constant.
        for (sourceSet in listOf("commonMain", "jvmMain", "wasmJsMain")) {
            fixture.writeKotlin(
                sourceSet,
                "com/example/${sourceSet.replaceFirstChar(Char::uppercase)}Sample.kt",
                """
                package com.example

                class ${sourceSet.replaceFirstChar(Char::uppercase)}Sample {
                    fun length(value: String): Int = value.length
                }
                """.trimIndent()
            )
        }
    }

    @Test
    @DisplayName("pins the JVM toolchain instead of compiling against whatever runs Gradle")
    fun pinsToolchain() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                platform {
                    javaVersion = JavaVersion.VERSION_$PINNED_JAVA
                    explicitApi = false
                    allWarningsAsErrors = false
                }

                project {
                    name = "Sample"
                    description = "Fixture"
                }
            """.trimIndent(),
            // A toolchain is not visible in a task outcome, so the build is asked what the JVM
            // target was actually configured with. `jvmToolchain` sets it; nothing else here does.
            //
            // Only this task is run, never a compilation, so no toolchain has to be provisioned -
            // which is what lets the pinned version be one the test machine does not have.
            extra = """
                tasks.register("printJvmTarget") {
                    val target = tasks
                        .named("compileKotlinJvm", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java)
                        .map { it.compilerOptions.jvmTarget.get().target }
                    doLast { println("jvm-target=" + target.get()) }
                }
            """.trimIndent()
        )

        val result = fixture.build("printJvmTarget")

        result.output shouldContain "jvm-target=$PINNED_JAVA"
    }

    @Test
    @DisplayName("locks a classpath per target rather than the two the JVM plugin would have")
    fun locksPerTargetClasspaths() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    dependencyLocking {
                        enabled = true
                    }
                }
            """.trimIndent()
        )

        val result = fixture.build("kreateResolveAndLockAll", "--write-locks")

        result.task(":kreateResolveAndLockAll")?.outcome shouldBe TaskOutcome.SUCCESS

        // `compileClasspath` and `runtimeClasspath` do not exist here. Before the per-target names
        // were derived, this file was written with nothing in it at all.
        val locked = fixture.file("gradle.lockfile").readText()
        locked shouldContain "jvmCompileClasspath"
        locked shouldContain "jvmRuntimeClasspath"
        locked shouldContain "wasmJsCompileClasspath"
        locked shouldContain "wasmJsRuntimeClasspath"
    }

    @Test
    @DisplayName("check analyses every source set instead of an aggregate task with no sources")
    fun checkAnalysesSourceSets() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    detekt {
                        enabled = true
                        allRules = false
                        buildUponDefaultConfig = true
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("dev.detekt")""")
        )
        fixture.write("detekt.yaml", "")

        val result = fixture.build("check")

        // The aggregate task has no sources under this plugin, so `check` depending on it alone is
        // a quality gate that reads nothing.
        result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":detektJvmMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":detektWasmJsMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("kreateDetekt analyses every source set, so a pipeline needs no list of task names")
    fun analyseTaskCoversEverySourceSet() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    detekt {
                        enabled = true
                        allRules = false
                        buildUponDefaultConfig = true
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("dev.detekt")""")
        )
        fixture.write("detekt.yaml", "")

        val result = fixture.build("kreateDetekt")

        // The point of the task: one name a CI job can run that reaches every target. Before it
        // existed, every project spelled these out by hand and a target added later was analysed
        // by nothing, silently.
        result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":detektJvmMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":detektWasmJsMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("gives each Detekt task its own report directory")
    fun reportsPerTask() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    detekt {
                        enabled = true
                        allRules = false

                        reports {
                            markdown {
                                required = true
                                outputLocation = layout.buildDirectory.file("reports/detekt/report.md")
                            }
                        }
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("dev.detekt")""")
        )
        fixture.write("detekt.yaml", "")

        fixture.build("check")

        // One shared path would have made these overlapping task outputs, and whichever ran last
        // would be the only report left.
        fixture.file("build/reports/detekt/detektCommonMainSourceSet/report.md").isFile shouldBe true
        fixture.file("build/reports/detekt/detektJvmMainSourceSet/report.md").isFile shouldBe true
        fixture.file("build/reports/detekt/detektWasmJsMainSourceSet/report.md").isFile shouldBe true
    }

    @Test
    @DisplayName("keeps generated sources out of static analysis")
    fun ignoresGeneratedSources() {
        fixture.writeMultiplatformBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    detekt {
                        enabled = true
                        allRules = false
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("dev.detekt")"""),
            // A generated file on the source set, the way KSP puts a test launcher there. It is
            // deliberately something Detekt would object to.
            extra = """
                val generate = tasks.register("generate") {
                    val output = layout.buildDirectory.dir("generated/sample")
                    outputs.dir(output)
                    doLast {
                        val file = output.get().file("com/example/Generated.kt").asFile
                        file.parentFile.mkdirs()
                        file.writeText(
                            "package com.example\n\nclass Generated { fun size(v: String): Int = v.length }   \n"
                        )
                    }
                }

                kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generate)
            """.trimIndent()
        )
        fixture.write(
            "detekt.yaml",
            """
            style:
              TrailingWhitespace:
                active: true
            """.trimIndent()
        )

        val result = fixture.build("check")

        result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
