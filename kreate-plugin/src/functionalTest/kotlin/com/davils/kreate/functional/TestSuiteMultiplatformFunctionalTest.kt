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

private const val JUNIT_VERSION: String = "6.1.3"

private val SAMPLE_SOURCE_SETS: List<String> = listOf("commonMain", "jvmMain", "wasmJsMain")

class TestSuiteMultiplatformFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(): KreateBuildFixture {
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

    context("test suites on Kotlin Multiplatform") {
        test("a test in the shared source set is compiled and run by the JVM target") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "SharedSuiteRan")
            fixture.writeTestingBuild()

            val result = fixture.build("jvmUnitTest")

            result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.reportsOf("jvmUnitTest") shouldContain "SharedSuiteRan"
        }

        test("a test in the per-target source set is run by that target's task") {
            val fixture = newBuild()
            fixture.writeSuiteTest("jvmUnitTest", "TargetSuiteRan")
            fixture.writeTestingBuild()

            fixture.build("jvmUnitTest")

            fixture.reportsOf("jvmUnitTest") shouldContain "TargetSuiteRan"
        }

        test("the default hierarchy survives, so nativeMain still exists") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "HierarchyCanary")
            fixture.writeTestingBuild(
                extra = """
                    kotlin {
                        linuxX64()
                        mingwX64()
                    }

                    tasks.register("printSourceSets") {
                        val names = kotlin.sourceSets.names.sorted().joinToString()
                        doLast { println("SOURCE_SETS=" + names) }
                    }
                """.trimIndent()
            )

            val output = fixture.build("printSourceSets").output

            val createdOnlyByTheDefaultHierarchy = "nativeMain"
            output shouldContain createdOnlyByTheDefaultHierarchy
            output shouldNotContain "KotlinDefaultHierarchyFallback"
        }

        test("names the per-target tasks the way the Kotlin plugin would") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "Named")
            fixture.writeTestingBuild()

            val output = fixture.build("tasks", "--all").output

            output shouldContain "jvmUnitTest"
            output shouldContain "jvmIntegrationTest"
        }

        test("the suite name runs every target's task") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "Aggregated")
            fixture.writeTestingBuild()

            val result = fixture.build("unitTest")

            result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.reportsOf("jvmUnitTest") shouldContain "Aggregated"
        }

        test("check runs the unit suite and leaves the integration suite alone") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "OnCheck")
            fixture.writeSuiteTest("commonIntegrationTest", "NotOnCheck")
            fixture.writeTestingBuild()

            val result = fixture.buildWithoutKotlinTestTasks("check")

            result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":jvmIntegrationTest") shouldBe null
        }

        test("the conventional jvmTest task is skipped") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "Replacement")
            fixture.writeTestingBuild()

            val resultWithJvmTestNamedOnCommandLine = fixture.buildWithoutKotlinTestTasks("check", "jvmTest")

            resultWithJvmTestNamedOnCommandLine.task(":jvmTest")?.outcome shouldBe TaskOutcome.SKIPPED
        }

        test("a target that cannot carry a suite is rejected instead of silently ignored") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "Rejected")
            fixture.writeTestingBuild(
                testsBlock = """
                    suites {
                        named("integrationTest") {
                            targets = listOf("wasmJs")
                        }
                    }
                """.trimIndent()
            )

            val output = fixture.buildAndFail("check").output

            output shouldContain "wasmJs"
            output shouldContain "cannot carry a named test suite"
        }

        test("a suite's test task is seen by the coverage engine") {
            val fixture = newBuild()
            fixture.write(
                "src/jvmUnitTest/kotlin/com/example/CoversJvmSample.kt",
                """
                package com.example

                import org.junit.jupiter.api.Test

                class CoversJvmSample {
                    @Test
                    fun covers() {
                        assert(JvmMainSample().length("ab") == 2)
                    }
                }
                """.trimIndent()
            )
            fixture.writeMultiplatformBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    project {
                        name = "Sample"

                        tests {
                            enabled = true
                            maxParallelForks = 1

                            suites {
                                named("unitTest") {
                                    dependencies {
                                        implementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                                    }
                                }
                            }
                        }

                        coverage {
                            enabled = true
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf("""id("org.jetbrains.kotlinx.kover")""")
            )

            fixture.build("koverXmlReport").task(":koverXmlReport")?.outcome shouldBe TaskOutcome.SUCCESS

            val report = fixture.file("build/reports/kover/report.xml").readText()
            report shouldContain "JvmMainSample"
            val somethingRecordedAsCovered = "covered="
            report shouldContain somethingRecordedAsCovered
            report shouldNotContain "CoversJvmSample"
        }

        test("reuses the configuration cache entry") {
            val fixture = newBuild()
            fixture.writeSuiteTest("commonUnitTest", "Cached")
            fixture.writeTestingBuild()

            fixture.buildWithoutKotlinTestTasks("check")

            fixture.buildWithoutKotlinTestTasks("check").output shouldContain "Configuration cache entry reused"
        }
    }
})

private fun KreateBuildFixture.writeTestingBuild(testsBlock: String = "", extra: String = "") {
    writeMultiplatformBuild(
        kreateBlock = """
            ${KreateBuildFixture.platformBlock}

            project {
                name = "Sample"

                tests {
                    enabled = true
                    maxParallelForks = 1

                    report {
                        enabled = true
                        xml = true
                    }

                    suites {
                        named("unitTest") {
                            dependencies {
                                implementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                            }
                        }

                        named("integrationTest") {
                            dependencies {
                                implementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                            }
                        }
                    }

                    $testsBlock
                }
            }
        """.trimIndent(),
        extra = extra
    )
}

private fun KreateBuildFixture.writeSuiteTest(sourceSet: String, className: String) {
    write(
        "src/$sourceSet/kotlin/com/example/$className.kt",
        """
        package com.example

        import org.junit.jupiter.api.Test

        class $className {
            @Test
            fun $className() {
                assert(CommonMainSample().length("ab") == 2)
            }
        }
        """.trimIndent()
    )
}

private fun KreateBuildFixture.reportsOf(taskName: String): String {
    val resultsDirectory = file("build/test-results/$taskName")
    val reports = resultsDirectory.walkTopDown().filter { report -> report.extension == "xml" }
    return reports.joinToString("") { report -> report.readText() }
}
