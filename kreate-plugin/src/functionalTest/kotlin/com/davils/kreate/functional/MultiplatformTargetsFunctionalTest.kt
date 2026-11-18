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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.util.jar.JarFile
import java.util.zip.ZipFile

private const val SETTINGS_WITH_TOOL_REPOSITORIES: String = """
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()

        ivy("https://nodejs.org/dist") {
            name = "Node.js"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }

        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.yarnpkg", "yarn")
            }
        }
    }
}

rootProject.name = "sample"
"""

private const val SAMPLE: String = """
package com.example

class Sample {
    fun length(value: String): Int = value.length
}
"""

private val MARKUP: Regex = Regex("<[^>]*>")

private val WHITESPACE: Regex = Regex("\\s+")

private fun KreateBuildFixture.reportSummaryOf(task: String): String {
    val html = file("build/reports/tests/$task/index.html").readText()
    val text = html.replace(MARKUP, " ")
    return text.replace(WHITESPACE, " ")
}

class MultiplatformTargetsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(settings: String = SETTINGS_WITH_TOOL_REPOSITORIES): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.write("settings.gradle.kts", settings)
        fixture.writeKotlin("commonMain", "com/example/Sample.kt", SAMPLE)
        return fixture
    }

    fun KreateBuildFixture.writeTargetsBuild(kreateBlock: String = "", extra: String = "") {
        write(
            "build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("com.davils.kreate")
            }

            group = "com.example"

            kreate {
                ${KreateBuildFixture.platformBlock}

                platform {
                    multiplatform {
                        toolsFromSettingsRepositories = true

                        targets {
                            jvm = true
                            js = setOf(com.davils.kreate.platform.WebEnvironment.NODE)
                            wasmJs = setOf(com.davils.kreate.platform.WebEnvironment.NODE)
                        }
                    }
                }

                $kreateBlock
            }

            $extra
            """.trimIndent()
        )
    }

    context("targets declared by Kreate") {
        test("declares the jvm, js and wasmJs targets the kreate block asks for") {
            val fixture = newBuild()
            fixture.writeTargetsBuild()

            val output = fixture.build("tasks", "--all").output

            output shouldContain "compileKotlinJvm"
            output shouldContain "jsNodeTest"
            output shouldContain "wasmJsNodeTest"
            output shouldNotContain "jsBrowserTest"
        }

        test("compiles every target from the settings repositories alone") {
            val fixture = newBuild()
            fixture.writeTargetsBuild()

            val result = fixture.build("assemble")

            result.task(":compileKotlinJs")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":compileKotlinWasmJs")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("fails when a project with the Kotlin JVM plugin asks for targets") {
            val fixture = newBuild()
            fixture.writeSettings()
            fixture.writeBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    platform {
                        multiplatform {
                            targets {
                                jvm = true
                            }
                        }
                    }
                """.trimIndent()
            )

            val result = fixture.buildAndFail("help")

            result.output shouldContain "asks Kreate for multiplatform targets"
        }
    }

    context("the Java module of the jvm target") {
        test("names jvmJar as an automatic module when jvmMain has no descriptor") {
            val fixture = newBuild()
            fixture.writeTargetsBuild(
                kreateBlock = """
                    platform {
                        jvm {
                            moduleName = "com.example.sample"
                        }
                    }
                """.trimIndent()
            )

            fixture.build("jvmJar")

            val libs = fixture.file("build/libs")
            val jars = libs.listFiles().orEmpty()
            val jar = jars.single { it.name.contains("-jvm") }
            val attributes = JarFile(jar).use { archive -> archive.manifest.mainAttributes }
            attributes.getValue("Automatic-Module-Name") shouldBe "com.example.sample"
        }

        test("compiles module-info.java of jvmMain against the classes of the Kotlin compiler") {
            val fixture = newBuild()
            fixture.write(
                "src/jvmMain/java/module-info.java",
                """
                module com.example.sample {
                    requires kotlin.stdlib;
                    exports com.example;
                }
                """.trimIndent()
            )
            fixture.writeTargetsBuild(
                kreateBlock = """
                    platform {
                        jvm {
                            moduleName = "com.example.sample"
                        }
                    }
                """.trimIndent()
            )

            val result = fixture.build("jvmJar")

            result.task(":compileJvmMainJava")?.outcome shouldBe TaskOutcome.SUCCESS
            val libs = fixture.file("build/libs")
            val jars = libs.listFiles().orEmpty()
            val jar = jars.single { it.name.contains("-jvm") }
            val entries = ZipFile(jar).use { zip -> zip.entries().toList() }
            val names = entries.map { entry -> entry.name }
            names shouldContain "module-info.class"
        }
    }

    context("publishing a multiplatform project") {
        test("creates a publication per target although Kreate applies its plugins after evaluation") {
            val fixture = newBuild()
            fixture.writeTargetsBuild(
                kreateBlock = """
                    project {
                        publish {
                            enabled = true
                        }
                    }
                """.trimIndent()
            )

            val output = fixture.build("tasks", "--all").output

            output shouldContain "generatePomFileForJvmPublication"
            output shouldContain "generatePomFileForWasmJsPublication"
        }
    }

    context("a suite shared with the JavaScript and Wasm targets") {
        fun KreateBuildFixture.writeSharedSuiteBuild(suites: String = "") {
            writeTargetsBuild(
                kreateBlock = """
                    project {
                        tests {
                            enabled = true

                            kotest {
                                enabled = true
                            }

                            suites {
                                named("unitTest") {
                                    nonJvmTargets = true
                                }
                                named("integrationTest") {
                                    enabled = false
                                }
                                $suites
                            }
                        }
                    }
                """.trimIndent()
            )
        }

        test("runs the Kotest specs of the shared source set on every target").config(tags = setOf(Slow)) {
            val fixture = newBuild()
            fixture.writeKotlin(
                "commonUnitTest",
                "com/example/SampleTest.kt",
                """
                package com.example

                import io.kotest.core.spec.style.FunSpec
                import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

                class SampleTest : FunSpec({
                    test("counts the characters") {
                        Sample().length("kreate") shouldBe 6
                    }
                })
                """.trimIndent()
            )
            fixture.writeSharedSuiteBuild()

            val result = fixture.build("unitTest")

            result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":jsNodeTest")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":wasmJsNodeTest")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.reportSummaryOf("jsNodeTest") shouldContain "1 tests 0 failures"
            fixture.reportSummaryOf("wasmJsNodeTest") shouldContain "1 tests 0 failures"
        }

        test("refuses a second suite that shares its tests with the other targets") {
            val fixture = newBuild()
            fixture.writeSharedSuiteBuild(
                suites = """
                    register("contractTest") {
                        nonJvmTargets = true
                    }
                """.trimIndent()
            )

            val result = fixture.buildAndFail("help")

            result.output shouldContain "all set nonJvmTargets = true"
        }
    }
})
