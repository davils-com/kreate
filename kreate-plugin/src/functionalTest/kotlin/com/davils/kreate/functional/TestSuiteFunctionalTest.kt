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
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome

private const val JUNIT_VERSION: String = "6.1.3"

private const val LEGACY_TEST_POLICY: String = "com.davils.kreate.testing.LegacyTestPolicy"

private const val TASK_NOT_FOUND: Int = -1

class TestSuiteFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        return fixture
    }

    context("test suites on Kotlin/JVM") {
        context("source sets and tasks") {
            test("runs the tests in src/unitTest/kotlin") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "UnitSuiteRan")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest"))

                val result = fixture.build("unitTest")

                result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
                fixture.executedTestReports("unitTest") shouldContain "UnitSuiteRan"
            }

            test("runs the tests in src/integrationTest/kotlin") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("integrationTest", "IntegrationSuiteRan")
                fixture.writeTestingBuild(testsBlock = junitFor("integrationTest"))

                fixture.build("integrationTest")

                fixture.executedTestReports("integrationTest") shouldContain "IntegrationSuiteRan"
            }

            test("a suite sees main's internal declarations") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "SeesInternals", body = "assert(secret() == \"internal\")")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest"))

                fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            test("a suite can carry dependencies the rest of the project does not") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.write(
                    "src/integrationTest/kotlin/com/example/UsesOwnDependency.kt",
                    """
                    package com.example

                    import org.apache.commons.lang3.StringUtils
                    import org.junit.jupiter.api.Test

                    class UsesOwnDependency {
                        @Test
                        fun usesOwnDependency() {
                            assert(StringUtils.reverse("ab") == "ba")
                        }
                    }
                    """.trimIndent()
                )
                fixture.writeTestingBuild(
                    testsBlock = junitFor(
                        "integrationTest",
                        dependencies = """implementation("org.apache.commons:commons-lang3:3.17.0")"""
                    )
                )

                fixture.build("integrationTest").task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            test("a suite can reuse another suite's fixtures") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.write(
                    "src/unitTest/kotlin/com/example/Fixtures.kt",
                    """
                    package com.example

                    internal object Fixtures {
                        val value: String = "shared"
                    }
                    """.trimIndent()
                )
                fixture.writeTest("integrationTest", "UsesFixtures", body = "assert(Fixtures.value == \"shared\")")
                fixture.writeTestingBuild(
                    testsBlock = junitFor("unitTest") +
                        "\n" +
                        junitFor(
                            "integrationTest",
                            body = """dependsOnSuites = listOf("unitTest")"""
                        )
                )

                fixture.build("integrationTest").task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            test("a registered suite gets a source set and a task of its own") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("contractTest", "ContractSuiteRan")
                fixture.writeTestingBuild(
                    testsBlock = """
                        suites {
                            register("contractTest") {
                                runOnCheck = false
                                dependencies {
                                    implementation("org.junit.jupiter:junit-jupiter:5.11.4")
                                }
                            }
                        }
                    """.trimIndent()
                )

                fixture.build("contractTest").task(":contractTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        context("check wiring") {
            test("check runs the unit suite and leaves the integration suite alone") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "OnCheck")
                fixture.writeTest("integrationTest", "NotOnCheck")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest", "integrationTest"))

                val result = fixture.build("check")

                result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
                result.task(":integrationTest") shouldBe null
            }

            test("asking for the integration suite does not drag the unit suite in") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "NotRequested")
                fixture.writeTest("integrationTest", "Requested")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest", "integrationTest"))

                val result = fixture.build("integrationTest")

                result.task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
                result.task(":unitTest") shouldBe null
            }

            test("the integration suite runs after the unit suite when both are asked for") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "First")
                fixture.writeTest("integrationTest", "Second")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest", "integrationTest"))

                val output = fixture.build("integrationTest", "unitTest").output

                val unitTestLine = output.taskLineIndex("unitTest")
                val integrationTestLine = output.taskLineIndex("integrationTest")
                unitTestLine shouldBeGreaterThanOrEqual 0
                integrationTestLine shouldBeGreaterThanOrEqual 0
                unitTestLine shouldBeLessThan integrationTestLine
            }

            test("a suite taken off check is not run by check") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "Skipped")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest", body = "runOnCheck = false"))

                fixture.build("check").task(":unitTest") shouldBe null
            }
        }

        context("tag filtering") {
            test("an excluded tag is not run, so the suite's own useJUnitPlatform call wins") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.write(
                    "src/unitTest/kotlin/com/example/Tagged.kt",
                    """
                    package com.example

                    import org.junit.jupiter.api.Tag
                    import org.junit.jupiter.api.Test

                    class Tagged {
                        @Test
                        fun fast() = assert(true)

                        @Test
                        @Tag("slow")
                        fun slow(): Unit = error("the excluded tag ran")
                    }
                    """.trimIndent()
                )
                fixture.writeTestingBuild(
                    testsBlock = junitFor("unitTest", body = """excludeTags = listOf("slow")""")
                )

                fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        context("legacy test source set") {
            test("the conventional test task is skipped by default") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "Replacement")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest"))

                val result = fixture.build("check", "test")

                result.task(":test")?.outcome shouldBe TaskOutcome.SKIPPED
            }

            test("the FAIL policy names the directory that still holds tests") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("test", "StillHere")
                fixture.writeTestingBuild(
                    testsBlock = legacyPolicy("FAIL"),
                    extra = """
                        dependencies {
                            testImplementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                            testRuntimeOnly("org.junit.platform:junit-platform-launcher:$JUNIT_VERSION")
                        }
                    """.trimIndent()
                )

                val output = fixture.buildAndFail("check").output

                val directoryWithForwardSlashesOnEveryPlatform = "src/test/kotlin"
                output shouldContain directoryWithForwardSlashesOnEveryPlatform
                output shouldContain "would stop running"
            }

            test("the FAIL policy passes once the tests have moved") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "Moved")
                fixture.writeTestingBuild(
                    testsBlock = legacyPolicy("FAIL") + "\n" + junitFor("unitTest")
                )

                fixture.build("check").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            test("the ALIAS policy runs an unmoved src/test tree as the unit suite") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("test", "AdoptedFromLegacy")
                fixture.writeTestingBuild(
                    testsBlock = legacyPolicy("ALIAS") + "\n" + junitFor("unitTest")
                )

                val result = fixture.build("test")

                result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
                fixture.executedTestReports("unitTest") shouldContain "AdoptedFromLegacy"
            }

            test("the KEEP policy leaves the conventional test task running") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("test", "LegacyStillRuns")
                fixture.writeTest("unitTest", "SuiteAlsoRuns")
                fixture.writeTestingBuild(
                    testsBlock = legacyPolicy("KEEP") + "\n" + junitFor("unitTest"),
                    extra = """
                        dependencies {
                            testImplementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                            testRuntimeOnly("org.junit.platform:junit-platform-launcher:$JUNIT_VERSION")
                        }
                    """.trimIndent()
                )

                val result = fixture.build("check")

                result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
                result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        context("Kotest bundle") {
            test("a Kotest spec runs with nothing declared but the bundle") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.write(
                    "src/unitTest/kotlin/com/example/GreeterSpec.kt",
                    """
                    package com.example

                    import io.kotest.core.spec.style.StringSpec
                    import io.kotest.matchers.shouldBe

                    class GreeterSpec : StringSpec({
                        "greets by name" {
                            Greeter().greet("world") shouldBe "Hello, world"
                        }
                    })
                    """.trimIndent()
                )
                fixture.writeTestingBuild(
                    testsBlock = """
                        kotest {
                            enabled = true
                        }
                    """.trimIndent()
                )

                fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
                fixture.executedTestReports("unitTest") shouldContain "greets by name"
            }
        }

        context("coverage") {
            test("measures the code a suite exercises and leaves the suite itself out") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest(
                    "unitTest",
                    "CoversGreeter",
                    body = "assert(Greeter().greet(\"x\") == \"Hello, x\")"
                )
                fixture.writeBuild(
                    kreateBlock = """
                        ${KreateBuildFixture.platformBlock}

                        project {
                            name = "Sample"

                            tests {
                                enabled = true
                                maxParallelForks = 1

                                ${junitFor("unitTest")}
                            }

                            coverage {
                                enabled = true
                            }
                        }
                    """.trimIndent(),
                    extraPlugins = listOf("""id("org.jetbrains.kotlinx.kover")"""),
                    extra = "repositories { mavenCentral() }"
                )

                fixture.build("koverXmlReport").task(":koverXmlReport")?.outcome shouldBe TaskOutcome.SUCCESS

                val report = fixture.file("build/reports/kover/report.xml").readText()
                report shouldContain "Greeter"
                val somethingRecordedAsCovered = "covered="
                report shouldContain somethingRecordedAsCovered
                val suiteClassOutsideProductionCode = "CoversGreeter"
                report shouldNotContain suiteClassOutsideProductionCode
            }
        }

        context("validation") {
            test("a suite cannot take over the production source set") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTestingBuild(
                    testsBlock = """
                        suites {
                            named("unitTest") { sourceSetName = "main" }
                        }
                    """.trimIndent()
                )

                fixture.buildAndFail("check").output shouldContain "the production source set"
            }

            test("a suite cannot depend on one that is not registered") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTestingBuild(
                    testsBlock = """
                        suites {
                            named("unitTest") { dependsOnSuites = listOf("nope") }
                        }
                    """.trimIndent()
                )

                fixture.buildAndFail("check").output shouldContain "which is not registered"
            }
        }

        context("build contract") {
            test("reuses the configuration cache entry") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeTest("unitTest", "Cached")
                fixture.writeTestingBuild(testsBlock = junitFor("unitTest"))

                fixture.build("check")

                fixture.build("check").output shouldContain "Configuration cache entry reused"
            }

            test("registers no suite tasks while testing is disabled") {
                val fixture = newBuild()
                fixture.writeProductionCode()
                fixture.writeBuild(
                    kreateBlock = """
                        ${KreateBuildFixture.platformBlock}

                        project {
                            name = "Sample"
                        }
                    """.trimIndent(),
                    extra = "repositories { mavenCentral() }"
                )

                val output = fixture.build("tasks", "--all").output

                output shouldNotContain "unitTest"
                output shouldNotContain "integrationTest"
            }
        }
    }
})

private fun KreateBuildFixture.writeTestingBuild(testsBlock: String = "", extra: String = "") {
    writeBuild(
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

                    $testsBlock
                }
            }
        """.trimIndent(),
        extra = """
            repositories { mavenCentral() }

            $extra
        """.trimIndent()
    )
}

private fun KreateBuildFixture.writeProductionCode() {
    writeKotlin(
        "com/example/Greeter.kt",
        """
        package com.example

        class Greeter {
            fun greet(name: String): String = "Hello, " + name
        }

        internal fun secret(): String = "internal"
        """.trimIndent()
    )
}

private fun KreateBuildFixture.writeTest(sourceSet: String, className: String, body: String = "assert(true)") {
    write(
        "src/$sourceSet/kotlin/com/example/$className.kt",
        """
        package com.example

        import org.junit.jupiter.api.Test

        class $className {
            @Test
            fun $className() {
                $body
            }
        }
        """.trimIndent()
    )
}

private fun KreateBuildFixture.executedTestReports(suite: String): String {
    val resultsDirectory = file("build/test-results/$suite")
    val reports = resultsDirectory.walkTopDown().filter { report -> report.extension == "xml" }
    return reports.joinToString("") { report -> report.readText() }
}

private fun junitFor(
    vararg suites: String,
    body: String = "",
    dependencies: String = ""
): String {
    val entries = suites.joinToString("\n\n") { suite ->
        """
        named("$suite") {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                $dependencies
            }
            $body
        }
        """.trimIndent()
    }
    return """
        suites {
            $entries
        }
    """.trimIndent()
}

private fun legacyPolicy(policy: String): String = "legacyTestSourceSet = $LEGACY_TEST_POLICY.$policy"

private fun String.taskLineIndex(taskName: String): Int {
    val ownExecutionLine = Regex("^> Task :$taskName\\b.*$", RegexOption.MULTILINE)
    return ownExecutionLine.find(this)?.range?.first ?: TASK_NOT_FOUND
}
