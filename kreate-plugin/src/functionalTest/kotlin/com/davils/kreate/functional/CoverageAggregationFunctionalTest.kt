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
 * Tests for merging the coverage of several projects into one report.
 *
 * Aggregation is the only part of the coverage feature that cannot be observed in a
 * single-project build, and it is also the part where a mistake is invisible rather than loud:
 * a report that silently covers one module instead of three still looks like a coverage report.
 */
@DisplayName("Coverage aggregation")
class CoverageAggregationFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
    }

    /**
     * Writes a settings file that includes the given subprojects.
     */
    private fun writeSettings(vararg subprojects: String) {
        fixture.write(
            "settings.gradle.kts",
            """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            rootProject.name = "sample"

            ${subprojects.joinToString("\n") { "include(\"$it\")" }}
            """.trimIndent()
        )
    }

    /**
     * Writes a subproject with one class, one test covering it, and optionally the Kover plugin.
     */
    private fun writeSubproject(name: String, className: String, applyKover: Boolean = true) {
        val koverPlugin = if (applyKover) """id("org.jetbrains.kotlinx.kover")""" else ""

        fixture.write(
            "$name/build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm")
                $koverPlugin
            }

            dependencies {
                testImplementation(kotlin("test"))
            }

            tasks.test {
                useJUnitPlatform()
            }
            """.trimIndent()
        )

        fixture.write(
            "$name/src/main/kotlin/com/example/$className.kt",
            """
            package com.example

            class $className {
                fun value(): String = "$className"
            }
            """.trimIndent()
        )

        fixture.write(
            "$name/src/test/kotlin/com/example/${className}Test.kt",
            """
            package com.example

            import kotlin.test.Test
            import kotlin.test.assertEquals

            class ${className}Test {
                @Test
                fun value() {
                    assertEquals("$className", $className().value())
                }
            }
            """.trimIndent()
        )
    }

    /**
     * Writes the aggregating root build.
     */
    private fun writeRoot(aggregateBlock: String) {
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Aggregation fixture"

                    coverage {
                        enabled = true

                        $aggregateBlock
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("org.jetbrains.kotlinx.kover")""")
        )
    }

    @Test
    @DisplayName("merges every subproject when no paths are listed")
    fun mergesAllSubprojects() {
        writeSettings(":core", ":api")
        writeSubproject("core", "Core")
        writeSubproject("api", "Api")
        writeRoot(
            """
            aggregate {
                enabled = true
            }
            """.trimIndent()
        )

        val result = fixture.build("koverXmlReport")

        result.task(":koverXmlReport")?.outcome shouldBe TaskOutcome.SUCCESS

        val report = fixture.file("build/reports/kover/report.xml").readText()
        report shouldContain "Core"
        report shouldContain "Api"
    }

    @Test
    @DisplayName("merges only the projects that were listed")
    fun mergesListedProjectsOnly() {
        // Listing paths explicitly is what keeps a module added later from silently changing
        // the number the gate is measured against.
        writeSettings(":core", ":api")
        writeSubproject("core", "Core")
        writeSubproject("api", "Api")
        writeRoot(
            """
            aggregate {
                enabled = true
                projects = listOf(":core")
            }
            """.trimIndent()
        )

        val result = fixture.build("koverXmlReport")

        val report = fixture.file("build/reports/kover/report.xml").readText()
        report shouldContain "Core"
        report shouldNotContain "Api"
    }

    @Test
    @DisplayName("applies Kover to an aggregated project that does not apply it itself")
    fun appliesKoverToAggregatedProjects() {
        // Aggregation names the projects to measure, and a project cannot contribute coverage
        // without the plugin. Listing it is the decision; applying the plugin there follows from
        // it, and is not a second thing every subproject's build script has to repeat.
        writeSettings(":core", ":api")
        writeSubproject("core", "Core")
        writeSubproject("api", "Api", applyKover = false)
        writeRoot(
            """
            aggregate {
                enabled = true
            }
            """.trimIndent()
        )

        val result = fixture.build("koverXmlReport")

        // The aggregated project never applies Kover itself; the aggregating project applies it
        // there, which is the only way it can contribute any coverage at all.
        result.task(":api:koverGenerateArtifact")?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.file("build/reports/kover/report.xml").readText() shouldContain "Api"
    }

    @Test
    @DisplayName("names a configured path that matches no project")
    fun reportsUnknownProjectPath() {
        writeSettings(":core")
        writeSubproject("core", "Core")
        writeRoot(
            """
            aggregate {
                enabled = true
                projects = listOf(":core", ":does-not-exist")
            }
            """.trimIndent()
        )

        val result = fixture.buildAndFail("koverXmlReport")

        result.output shouldContain ":does-not-exist"
        result.output shouldContain "does not exist in this build"
    }

    @Test
    @DisplayName("gates the merged coverage rather than each project separately")
    fun verifiesMergedCoverage() {
        // The point of aggregating: a bound that answers for the product, not for whichever
        // module happens to be best tested.
        writeSettings(":core")
        writeSubproject("core", "Core")
        writeRoot(
            """
            aggregate {
                enabled = true
            }

            verify {
                minLineCoverage = 100
            }
            """.trimIndent()
        )

        val result = fixture.build("koverVerify")

        result.task(":koverVerify")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
