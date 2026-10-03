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
import org.gradle.testkit.runner.TaskOutcome

private const val KOVER_PLUGIN: String = """id("org.jetbrains.kotlinx.kover")"""

class PluginContractFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    val minimalKreateBlock = """
        ${KreateBuildFixture.platformBlock}

        project {
            name = "Sample"
            description = "Fixture"
        }
    """.trimIndent()

    fun newBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    context("Plugin contract") {
        test("applies cleanly with no feature enabled") {
            val fixture = newBuild()
            fixture.writeBuild(minimalKreateBlock)

            val result = fixture.build("build")

            result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("registers no feature tasks when nothing is enabled") {
            val fixture = newBuild()
            fixture.writeBuild(minimalKreateBlock)

            val result = fixture.build("tasks", "--all")

            val consumerTaskList = result.output
            consumerTaskList shouldNotContain "kreateJniBuild"
            consumerTaskList shouldNotContain "kreateTrivyScan"
            consumerTaskList shouldNotContain "koverXmlReport"
        }

        test("reuses the configuration cache on a second run") {
            val fixture = newBuild()
            fixture.writeBuild(minimalKreateBlock)
            fixture.build("build")

            val result = fixture.build("build")

            result.output shouldContain "Configuration cache entry reused"
        }

        test("generates build constants and compiles them") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                project {
                    buildConstant {
                        enabled = true
                        className = "SampleConstants"
                        path = "generated/constants"

                        constant("flavour", "enterprise")
                    }
                }
                """.trimIndent()
            )

            val result = fixture.build("build")

            result.task(":kreateBuildConstants")?.outcome shouldBe TaskOutcome.SUCCESS
            val generatedDirectory = fixture.file("build/generated/constants")
            val generated = generatedDirectory.walkTopDown().single { file -> file.name == "SampleConstants.kt" }
            generated.readText() shouldContain "FLAVOUR"
            generated.readText() shouldContain "enterprise"
        }

        test("registers the Trivy scan tasks when the feature is enabled") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                trivy {
                    enabled = true
                }
                """.trimIndent()
            )

            val result = fixture.build("tasks", "--all")

            result.output shouldContain "kreateTrivyScan"
            result.output shouldContain "kreateTrivySecretScan"
            result.output shouldContain "kreateTrivyLicenseScan"
            result.output shouldContain "kreateTrivyVulnerabilityScan"
        }

        test("skips a Trivy scan with an actionable message when no lock files exist") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                trivy {
                    enabled = true
                }
                """.trimIndent()
            )

            val result = fixture.build("kreateTrivyVulnerabilityScan")

            val howToGenerateLockFiles = "--write-locks"
            result.output shouldContain howToGenerateLockFiles
        }

        test("one plugins block is enough for every feature at once") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                project {
                    detekt { enabled = true }
                    coverage { enabled = true }
                    benchmark { enabled = true }

                    publish {
                        enabled = true

                        repositories {
                            mavenCentral { enabled = true }
                        }
                    }
                }
                """.trimIndent()
            )

            val result = fixture.build("tasks", "--all")

            result.output shouldContain "detektMainSourceSet"
            result.output shouldContain "koverXmlReport"
            result.output shouldContain "benchmarksBenchmarkGenerate"
            result.output shouldContain "publishToMavenLocal"
        }

        test("a plugin the consumer applies themselves is left alone") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                project {
                    detekt { enabled = true }
                    coverage { enabled = true }
                }
                """.trimIndent(),
                extraPlugins = listOf("""id("dev.detekt")""", KOVER_PLUGIN),
                extra = """
                    detekt {
                        buildUponDefaultConfig = true
                    }
                """.trimIndent()
            )

            val result = fixture.build("tasks", "--all")

            result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "detektMainSourceSet"
            result.output shouldContain "koverXmlReport"
        }

        test("applies the Detekt plugin itself when Detekt is enabled") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                project {
                    detekt {
                        enabled = true
                    }
                }
                """.trimIndent()
            )

            val result = fixture.build("tasks", "--all")

            result.output shouldContain "detektMainSourceSet"
            result.output shouldContain "kreateDetekt"
        }

        test("applies the Kover plugin itself when coverage is enabled") {
            val fixture = newBuild()
            fixture.writeBuild(
                """
                $minimalKreateBlock

                project {
                    coverage {
                        enabled = true
                    }
                }
                """.trimIndent()
            )

            val result = fixture.build("tasks", "--all")

            result.output shouldContain "koverXmlReport"
            result.output shouldContain "koverVerify"
        }

        test("measures coverage of the code a test actually exercises") {
            val fixture = newBuild()
            fixture.writeKotlin(
                "com/example/Covered.kt",
                """
                package com.example

                class Covered {
                    fun greet(): String = "hello"
                }
                """.trimIndent()
            )
            fixture.write(
                "src/test/kotlin/com/example/CoveredTest.kt",
                """
                package com.example

                import kotlin.test.Test
                import kotlin.test.assertEquals

                class CoveredTest {
                    @Test
                    fun greets() {
                        assertEquals("hello", Covered().greet())
                    }
                }
                """.trimIndent()
            )

            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN),
                extra = """
                    dependencies {
                        testImplementation(kotlin("test"))
                    }

                    tasks.test {
                        useJUnitPlatform()
                    }
                """.trimIndent()
            )

            val result = fixture.build("koverXmlReport")

            result.task(":koverXmlReport")?.outcome shouldBe TaskOutcome.SUCCESS

            val report = fixture.file("build/reports/kover/report.xml").readText()
            report shouldContain "Covered"
            val somethingRecordedAsCovered = "covered="
            report shouldContain somethingRecordedAsCovered
        }

        test("fails the build when coverage is below the configured bound") {
            val fixture = newBuild()
            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true

                            verify {
                                minLineCoverage = 90
                            }
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN)
            )

            val result = fixture.buildAndFail("koverVerify")

            result.task(":koverVerify")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Minimum line coverage"
        }

        test("runs the coverage gate as part of check") {
            val fixture = newBuild()
            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true

                            verify {
                                minLineCoverage = 0
                            }
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN)
            )

            val result = fixture.build("check")

            result.task(":koverVerify")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("registers no verification rule for a bound that was never set") {
            val fixture = newBuild()
            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN)
            )

            val result = fixture.build("koverVerify")

            result.task(":koverVerify")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldNotContain "Minimum line coverage"
        }

        test("enforces a named rule with its own grouping and bounds") {
            val fixture = newBuild()
            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true

                            verify {
                                rules {
                                    create("Every class carries its own weight") {
                                        groupBy = com.davils.kreate.coverage.Grouping.CLASS
                                        minBound(80, com.davils.kreate.coverage.CoverageUnit.LINE)
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN)
            )

            val result = fixture.buildAndFail("koverVerify")

            result.output shouldContain "Every class carries its own weight"
        }

        test("rejects a named rule that declares no bounds") {
            val fixture = newBuild()
            fixture.writeBuild(
                kreateBlock = """
                    $minimalKreateBlock

                    project {
                        coverage {
                            enabled = true

                            verify {
                                rules {
                                    create("Checks nothing")
                                }
                            }
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf(KOVER_PLUGIN)
            )

            val result = fixture.buildAndFail("koverVerify")

            result.output shouldContain "declares no bounds"
        }
    }
})
