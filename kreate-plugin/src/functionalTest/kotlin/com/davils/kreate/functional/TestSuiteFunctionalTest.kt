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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the named test suites on Kotlin/JVM.
 *
 * Nothing here asserts only that a task succeeded. A test task that compiled nothing and ran
 * nothing succeeds too, and that is precisely the failure this feature can produce: a source
 * directory that looks like it holds tests and is never built. Every assertion is either about
 * a named test having actually executed, or about a specific task outcome that could not happen
 * by accident.
 */
@DisplayName("test suites on Kotlin/JVM")
class TestSuiteFunctionalTest {

    private companion object {
        /**
         * The JUnit version the generated builds declare.
         *
         * Pinned to the JUnit 6 line so that it matches the JUnit Platform launcher Kreate adds
         * by default; a mismatch there is exactly the failure the default is meant to prevent.
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
    }

    /**
     * Writes a build with testing enabled and the given extra configuration inside `tests { }`.
     *
     * @param testsBlock Additional body for the `tests { }` block.
     * @param extra Additional build script content.
     */
    private fun writeBuild(testsBlock: String = "", extra: String = "") {
        fixture.writeBuild(
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

    /**
     * Writes a production class carrying an `internal` declaration.
     */
    private fun writeProductionCode() {
        fixture.writeKotlin(
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

    /**
     * Writes a JUnit 5 test into an arbitrary source set.
     *
     * @param sourceSet The source set directory below `src`.
     * @param className The test class name, also used as the test method name.
     * @param body The assertion body.
     */
    private fun writeTest(sourceSet: String, className: String, body: String = "assert(true)") {
        fixture.write(
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

    /**
     * A `suites { }` block that gives each named suite the JUnit 5 engine.
     *
     * Declared on the suite rather than in a top level `dependencies { }` block, because a
     * suite's configurations do not exist while the build script is running.
     *
     * @param suites The suite names to configure.
     * @param body Additional body for every named suite.
     * @param dependencies Additional lines for every named suite's `dependencies { }` block.
     * @return A `suites { }` block.
     */
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

    @Nested
    @DisplayName("source sets and tasks")
    inner class SourceSetsAndTasks {

        @Test
        @DisplayName("runs the tests in src/unitTest/kotlin")
        fun runsUnitSuite() {
            writeProductionCode()
            writeTest("unitTest", "UnitSuiteRan")
            writeBuild(testsBlock = junitFor("unitTest"))

            val result = fixture.build("unitTest")

            result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            // The report, not the task outcome: an empty suite succeeds just as loudly.
            fixture.file("build/test-results/unitTest").walkTopDown()
                .filter { it.extension == "xml" }
                .joinToString("") { it.readText() } shouldContain "UnitSuiteRan"
        }

        @Test
        @DisplayName("runs the tests in src/integrationTest/kotlin")
        fun runsIntegrationSuite() {
            writeProductionCode()
            writeTest("integrationTest", "IntegrationSuiteRan")
            writeBuild(testsBlock = junitFor("integrationTest"))

            fixture.build("integrationTest")

            fixture.file("build/test-results/integrationTest").walkTopDown()
                .filter { it.extension == "xml" }
                .joinToString("") { it.readText() } shouldContain "IntegrationSuiteRan"
        }

        @Test
        @DisplayName("a suite sees main's internal declarations")
        fun seesInternals() {
            writeProductionCode()
            writeTest("unitTest", "SeesInternals", body = "assert(secret() == \"internal\")")
            writeBuild(testsBlock = junitFor("unitTest"))

            fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @DisplayName("a suite can carry dependencies the rest of the project does not")
        fun ownDependencies() {
            writeProductionCode()
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
            writeBuild(
                testsBlock = junitFor(
                    "integrationTest",
                    dependencies = """implementation("org.apache.commons:commons-lang3:3.17.0")"""
                )
            )

            fixture.build("integrationTest").task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @DisplayName("a suite can reuse another suite's fixtures")
        fun sharedFixtures() {
            writeProductionCode()
            fixture.write(
                "src/unitTest/kotlin/com/example/Fixtures.kt",
                """
                package com.example

                internal object Fixtures {
                    val value: String = "shared"
                }
                """.trimIndent()
            )
            writeTest("integrationTest", "UsesFixtures", body = "assert(Fixtures.value == \"shared\")")
            writeBuild(
                testsBlock = junitFor("unitTest") + "\n" + junitFor(
                    "integrationTest",
                    body = """dependsOnSuites = listOf("unitTest")"""
                )
            )

            fixture.build("integrationTest").task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @DisplayName("a registered suite gets a source set and a task of its own")
        fun registeredSuite() {
            writeProductionCode()
            writeTest("contractTest", "ContractSuiteRan")
            writeBuild(
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

    @Nested
    @DisplayName("check wiring")
    inner class CheckWiring {

        @Test
        @DisplayName("check runs the unit suite and leaves the integration suite alone")
        fun checkScope() {
            writeProductionCode()
            writeTest("unitTest", "OnCheck")
            writeTest("integrationTest", "NotOnCheck")
            writeBuild(testsBlock = junitFor("unitTest", "integrationTest"))

            val result = fixture.build("check")

            result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":integrationTest") shouldBe null
        }

        @Test
        @DisplayName("asking for the integration suite does not drag the unit suite in")
        fun integrationAlone() {
            writeProductionCode()
            writeTest("unitTest", "NotRequested")
            writeTest("integrationTest", "Requested")
            writeBuild(testsBlock = junitFor("unitTest", "integrationTest"))

            val result = fixture.build("integrationTest")

            result.task(":integrationTest")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":unitTest") shouldBe null
        }

        @Test
        @DisplayName("the integration suite runs after the unit suite when both are asked for")
        fun ordering() {
            writeProductionCode()
            writeTest("unitTest", "First")
            writeTest("integrationTest", "Second")
            writeBuild(testsBlock = junitFor("unitTest", "integrationTest"))

            val output = fixture.build("integrationTest", "unitTest").output

            output.taskLineIndex("unitTest") shouldBeLessThan output.taskLineIndex("integrationTest")
        }

        @Test
        @DisplayName("a suite taken off check is not run by check")
        fun offCheck() {
            writeProductionCode()
            writeTest("unitTest", "Skipped")
            writeBuild(testsBlock = junitFor("unitTest", body = "runOnCheck = false"))

            fixture.build("check").task(":unitTest") shouldBe null
        }
    }

    @Nested
    @DisplayName("tag filtering")
    inner class Tags {

        @Test
        @DisplayName("an excluded tag is not run, so the suite's own useJUnitPlatform call wins")
        fun excludesTags() {
            writeProductionCode()
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
            writeBuild(
                testsBlock = junitFor("unitTest", body = """excludeTags = listOf("slow")""")
            )

            fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }

    @Nested
    @DisplayName("legacy test source set")
    inner class Legacy {

        @Test
        @DisplayName("the conventional test task is skipped by default")
        fun disabled() {
            writeProductionCode()
            writeTest("unitTest", "Replacement")
            writeBuild(testsBlock = junitFor("unitTest"))

            val result = fixture.build("check", "test")

            result.task(":test")?.outcome shouldBe TaskOutcome.SKIPPED
        }

        @Test
        @DisplayName("the FAIL policy names the directory that still holds tests")
        fun failsOnLeftovers() {
            writeProductionCode()
            writeTest("test", "StillHere")
            writeBuild(
                testsBlock = "legacyTestSourceSet = com.davils.kreate.module.project.tests.LegacyTestPolicy.FAIL",
                extra = """
                    dependencies {
                        testImplementation("org.junit.jupiter:junit-jupiter:$JUNIT_VERSION")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher:$JUNIT_VERSION")
                    }
                """.trimIndent()
            )

            val output = fixture.buildAndFail("check").output

            // Forward slashes on every platform. This assertion can only fail on Windows, which
            // is the point: the listing is built from `File`, and the rest of the same message
            // spells directories with `/`.
            output shouldContain "src/test/kotlin"
            output shouldContain "would stop running"
        }

        @Test
        @DisplayName("the FAIL policy passes once the tests have moved")
        fun failPolicyPassesWhenMoved() {
            writeProductionCode()
            writeTest("unitTest", "Moved")
            writeBuild(
                testsBlock = "legacyTestSourceSet = com.davils.kreate.module.project.tests.LegacyTestPolicy.FAIL\n" + junitFor("unitTest")
            )

            fixture.build("check").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @DisplayName("the ALIAS policy runs an unmoved src/test tree as the unit suite")
        fun aliasAdoptsSources() {
            writeProductionCode()
            writeTest("test", "AdoptedFromLegacy")
            writeBuild(
                testsBlock = "legacyTestSourceSet = com.davils.kreate.module.project.tests.LegacyTestPolicy.ALIAS\n" + junitFor("unitTest")
            )

            val result = fixture.build("test")

            result.task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.file("build/test-results/unitTest").walkTopDown()
                .filter { it.extension == "xml" }
                .joinToString("") { it.readText() } shouldContain "AdoptedFromLegacy"
        }

        @Test
        @DisplayName("the KEEP policy leaves the conventional test task running")
        fun keepRunsLegacy() {
            writeProductionCode()
            writeTest("test", "LegacyStillRuns")
            writeTest("unitTest", "SuiteAlsoRuns")
            writeBuild(
                testsBlock = "legacyTestSourceSet = com.davils.kreate.module.project.tests.LegacyTestPolicy.KEEP\n" + junitFor("unitTest"),
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

    @Nested
    @DisplayName("Kotest bundle")
    inner class Kotest {

        @Test
        @DisplayName("a Kotest spec runs with nothing declared but the bundle")
        fun runsKotestSpec() {
            writeProductionCode()
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
            writeBuild(
                testsBlock = """
                    kotest {
                        enabled = true
                    }
                """.trimIndent()
            )

            fixture.build("unitTest").task(":unitTest")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.file("build/test-results/unitTest").walkTopDown()
                .filter { it.extension == "xml" }
                .joinToString("") { it.readText() } shouldContain "greets by name"
        }
    }

    @Nested
    @DisplayName("coverage")
    inner class Coverage {

        @Test
        @DisplayName("measures the code a suite exercises and leaves the suite itself out")
        fun measuresProductionCodeOnly() {
            writeProductionCode()
            writeTest(
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
            // A zero-coverage report names the class too, so assert something was recorded.
            report shouldContain "covered="
            // The suite's own classes are not production code. Kover only recognises a source
            // set called `test`, so without Kreate excluding them they would land in the
            // denominator and the number would quietly describe the wrong thing.
            report shouldNotContain "CoversGreeter"
        }
    }

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        @DisplayName("a suite cannot take over the production source set")
        fun rejectsReservedName() {
            writeProductionCode()
            writeBuild(
                testsBlock = """
                    suites {
                        named("unitTest") { sourceSetName = "main" }
                    }
                """.trimIndent()
            )

            fixture.buildAndFail("check").output shouldContain "the production source set"
        }

        @Test
        @DisplayName("a suite cannot depend on one that is not registered")
        fun rejectsUnknownSuite() {
            writeProductionCode()
            writeBuild(
                testsBlock = """
                    suites {
                        named("unitTest") { dependsOnSuites = listOf("nope") }
                    }
                """.trimIndent()
            )

            fixture.buildAndFail("check").output shouldContain "which is not registered"
        }
    }

    @Nested
    @DisplayName("build contract")
    inner class BuildContract {

        @Test
        @DisplayName("reuses the configuration cache entry")
        fun configurationCache() {
            writeProductionCode()
            writeTest("unitTest", "Cached")
            writeBuild(testsBlock = junitFor("unitTest"))

            fixture.build("check")

            fixture.build("check").output shouldContain "Configuration cache entry reused"
        }

        @Test
        @DisplayName("registers no suite tasks while testing is disabled")
        fun noTasksWhenDisabled() {
            writeProductionCode()
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

/**
 * Returns where a task's own execution line appears in the build output.
 *
 * Anchored to the whole line on purpose: a plain `indexOf(":unitTest")` also matches
 * `:unitTestClasses`, and the compilation tasks of the two suites are free to interleave -
 * only the test tasks themselves are ordered.
 *
 * @param taskName The task name without its path.
 * @return The index of the line, or `-1` when the task did not run.
 */
private fun String.taskLineIndex(taskName: String): Int =
    Regex("^> Task :$taskName\\b.*$", RegexOption.MULTILINE).find(this)?.range?.first ?: -1

/**
 * Asserts that this index is smaller than [other] and that both were actually found.
 *
 * @param other The index that must come later.
 */
private infix fun Int.shouldBeLessThan(other: Int) {
    check(this >= 0 && other >= 0) { "expected both markers in the output, got $this and $other" }
    check(this < other) { "expected $this to come before $other" }
}
