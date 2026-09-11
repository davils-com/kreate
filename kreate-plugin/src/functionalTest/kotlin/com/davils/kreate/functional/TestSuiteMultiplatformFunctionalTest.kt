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
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the named test suites on Kotlin Multiplatform.
 *
 * Two of these are canaries rather than feature tests. Creating a source set tree by hand is
 * one `dependsOn` call away from switching off the Kotlin plugin's default hierarchy for the
 * whole project - `nativeMain` and every other shared source set simply stop existing, and the
 * build stays green while it happens. And a suite whose shared source set is never wired into
 * a compilation gives a directory that looks like tests and is never run. Both are asserted
 * here directly, because neither shows up as a failure anywhere else.
 */
@DisplayName("test suites on Kotlin Multiplatform")
class TestSuiteMultiplatformFunctionalTest {

    private companion object {
        /**
         * The JUnit version the generated builds declare.
         */
        const val JUNIT_VERSION: String = "6.1.3"
    }

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()

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

    /**
     * Writes a multiplatform build with testing enabled.
     *
     * @param testsBlock Additional body for the `tests { }` block.
     * @param extra Additional build script content.
     */
    private fun writeBuild(testsBlock: String = "", extra: String = "") {
        fixture.writeMultiplatformBuild(
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

    /**
     * Writes a JUnit 5 test into a multiplatform source set.
     *
     * @param sourceSet The source set name.
     * @param className The test class name, also used as the test method name.
     */
    private fun writeTest(sourceSet: String, className: String) {
        fixture.write(
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

    /**
     * Reads the XML reports of a test task.
     *
     * @param taskName The test task name.
     * @return The concatenated report contents.
     */
    private fun reportsOf(taskName: String): String =
        fixture.file("build/test-results/$taskName")
            .walkTopDown()
            .filter { it.extension == "xml" }
            .joinToString("") { it.readText() }

    @Test
    @DisplayName("a test in the shared source set is compiled and run by the JVM target")
    fun sharedSourceSetRuns() {
        writeTest("commonUnitTest", "SharedSuiteRan")
        writeBuild()

        val result = fixture.build("jvmUnitTest")

        result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        // The report rather than the outcome: a shared source set that is wired to no
        // compilation produces a task that succeeds without running anything.
        reportsOf("jvmUnitTest") shouldContain "SharedSuiteRan"
    }

    @Test
    @DisplayName("a test in the per-target source set is run by that target's task")
    fun targetSourceSetRuns() {
        writeTest("jvmUnitTest", "TargetSuiteRan")
        writeBuild()

        fixture.build("jvmUnitTest")

        reportsOf("jvmUnitTest") shouldContain "TargetSuiteRan"
    }

    @Test
    @DisplayName("the default hierarchy survives, so nativeMain still exists")
    fun hierarchySurvives() {
        writeTest("commonUnitTest", "HierarchyCanary")
        writeBuild(
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

        // nativeMain is created by the default hierarchy template and by nothing else. Its
        // absence is the signature of a manually added refines edge somewhere in the build.
        output shouldContain "nativeMain"
        output shouldNotContain "KotlinDefaultHierarchyFallback"
    }

    @Test
    @DisplayName("names the per-target tasks the way the Kotlin plugin would")
    fun taskNames() {
        writeTest("commonUnitTest", "Named")
        writeBuild()

        val output = fixture.build("tasks", "--all").output

        output shouldContain "jvmUnitTest"
        output shouldContain "jvmIntegrationTest"
    }

    @Test
    @DisplayName("the suite name runs every target's task")
    fun aggregateTask() {
        writeTest("commonUnitTest", "Aggregated")
        writeBuild()

        val result = fixture.build("unitTest")

        result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        reportsOf("jvmUnitTest") shouldContain "Aggregated"
    }

    @Test
    @DisplayName("check runs the unit suite and leaves the integration suite alone")
    fun checkScope() {
        writeTest("commonUnitTest", "OnCheck")
        writeTest("commonIntegrationTest", "NotOnCheck")
        writeBuild()

        val result = fixture.build("check")

        result.task(":jvmUnitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":jvmIntegrationTest") shouldBe null
    }

    @Test
    @DisplayName("the conventional jvmTest task is skipped")
    fun legacyDisabled() {
        writeTest("commonUnitTest", "Replacement")
        writeBuild()

        fixture.build("check", "jvmTest").task(":jvmTest")?.outcome shouldBe TaskOutcome.SKIPPED
    }

    @Test
    @DisplayName("a target that cannot carry a suite is rejected instead of silently ignored")
    fun rejectsUnsupportedTarget() {
        writeTest("commonUnitTest", "Rejected")
        writeBuild(
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

    @Test
    @DisplayName("a suite's test task is seen by the coverage engine")
    fun coverageSeesSuiteTasks() {
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
        // The suite's tasks are KotlinJvmTest tasks carrying a target name, which is the only
        // shape the coverage engine's multiplatform locator recognises. A plain Test task here
        // would leave every class at zero while the tests pass.
        report shouldContain "covered="
        report shouldNotContain "CoversJvmSample"
    }

    @Test
    @DisplayName("reuses the configuration cache entry")
    fun configurationCache() {
        writeTest("commonUnitTest", "Cached")
        writeBuild()

        fixture.build("check")

        fixture.build("check").output shouldContain "Configuration cache entry reused"
    }
}
