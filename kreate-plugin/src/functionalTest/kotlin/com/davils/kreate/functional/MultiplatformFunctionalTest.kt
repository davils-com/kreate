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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

private val PINNABLE_JAVA_VERSIONS: List<Int> = listOf(17, 21)

private val SAMPLE_SOURCE_SETS: List<String> = listOf("commonMain", "jvmMain", "wasmJsMain")

private const val DETEKT_PLUGIN: String = """id("dev.detekt")"""

class MultiplatformFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    val javaVersionDifferentFromTheRunningOne: Int =
        PINNABLE_JAVA_VERSIONS.first { version -> version != KreateBuildFixture.javaVersion }

    fun newBuildWithSourcePerSet(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()

        for (sourceSet in SAMPLE_SOURCE_SETS) {
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
        return fixture
    }

    fun checkWithoutTestExecution(fixture: KreateBuildFixture): BuildResult =
        fixture.buildWithoutKotlinTestTasks("check")

    context("Kotlin Multiplatform") {
        test("pins the JVM toolchain instead of compiling against whatever runs Gradle") {
            val fixture = newBuildWithSourcePerSet()
            val pinnedJava = javaVersionDifferentFromTheRunningOne
            fixture.writeMultiplatformBuild(
                kreateBlock = """
                    platform {
                        javaVersion = JavaVersion.VERSION_$pinnedJava
                        explicitApi = false
                        allWarningsAsErrors = false
                    }

                    project {
                        name = "Sample"
                        description = "Fixture"
                    }
                """.trimIndent(),
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

            result.output shouldContain "jvm-target=$pinnedJava"
        }

        test("locks a classpath per target rather than the two the JVM plugin would have") {
            val fixture = newBuildWithSourcePerSet()
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

            val locked = fixture.file("gradle.lockfile").readText()
            locked shouldContain "jvmCompileClasspath"
            locked shouldContain "jvmRuntimeClasspath"
            locked shouldContain "wasmJsCompileClasspath"
            locked shouldContain "wasmJsRuntimeClasspath"
        }

        test("check analyses every source set instead of an aggregate task with no sources") {
            val fixture = newBuildWithSourcePerSet()
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
                extraPlugins = listOf(DETEKT_PLUGIN)
            )
            fixture.write("detekt.yaml", "")

            val result = checkWithoutTestExecution(fixture)

            result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":detektJvmMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":detektWasmJsMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("kreateDetekt analyses every source set, so a pipeline needs no list of task names") {
            val fixture = newBuildWithSourcePerSet()
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
                extraPlugins = listOf(DETEKT_PLUGIN)
            )
            fixture.write("detekt.yaml", "")

            val result = fixture.build("kreateDetekt")

            result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":detektJvmMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":detektWasmJsMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("gives each Detekt task its own report directory") {
            val fixture = newBuildWithSourcePerSet()
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
                extraPlugins = listOf(DETEKT_PLUGIN)
            )
            fixture.write("detekt.yaml", "")

            checkWithoutTestExecution(fixture)

            fixture.file("build/reports/detekt/detektCommonMainSourceSet/report.md").isFile shouldBe true
            fixture.file("build/reports/detekt/detektJvmMainSourceSet/report.md").isFile shouldBe true
            fixture.file("build/reports/detekt/detektWasmJsMainSourceSet/report.md").isFile shouldBe true
        }

        test("keeps generated sources out of static analysis") {
            val fixture = newBuildWithSourcePerSet()
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
                extraPlugins = listOf(DETEKT_PLUGIN),
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

            val result = checkWithoutTestExecution(fixture)

            result.task(":detektCommonMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }
})
