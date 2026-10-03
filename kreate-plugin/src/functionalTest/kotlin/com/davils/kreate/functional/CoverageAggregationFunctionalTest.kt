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

class CoverageAggregationFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    context("Coverage aggregation") {
        test("merges every subproject when no paths are listed") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettingsIncluding(":core", ":api")
            fixture.writeSubproject("core", "Core")
            fixture.writeSubproject("api", "Api")
            fixture.writeRoot(
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

        test("merges only the projects that were listed") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettingsIncluding(":core", ":api")
            fixture.writeSubproject("core", "Core")
            fixture.writeSubproject("api", "Api")
            fixture.writeRoot(
                """
                aggregate {
                    enabled = true
                    projects = listOf(":core")
                }
                """.trimIndent()
            )

            fixture.build("koverXmlReport")

            val report = fixture.file("build/reports/kover/report.xml").readText()
            report shouldContain "Core"
            report shouldNotContain "Api"
        }

        test("applies Kover to an aggregated project that does not apply it itself") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettingsIncluding(":core", ":api")
            fixture.writeSubproject("core", "Core")
            fixture.writeSubproject("api", "Api", isKoverApplied = false)
            fixture.writeRoot(
                """
                aggregate {
                    enabled = true
                }
                """.trimIndent()
            )

            val result = fixture.build("koverXmlReport")

            result.task(":api:koverGenerateArtifact")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.file("build/reports/kover/report.xml").readText() shouldContain "Api"
        }

        test("names a configured path that matches no project") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettingsIncluding(":core")
            fixture.writeSubproject("core", "Core")
            fixture.writeRoot(
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

        test("gates the merged coverage rather than each project separately") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettingsIncluding(":core")
            fixture.writeSubproject("core", "Core")
            fixture.writeRoot(
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
})

private fun KreateBuildFixture.writeSettingsIncluding(vararg subprojects: String) {
    write(
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

private fun KreateBuildFixture.writeSubproject(name: String, className: String, isKoverApplied: Boolean = true) {
    val koverPlugin = KOVER_PLUGIN.takeIf { isKoverApplied }.orEmpty()

    write(
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

    write(
        "$name/src/main/kotlin/com/example/$className.kt",
        """
        package com.example

        class $className {
            fun value(): String = "$className"
        }
        """.trimIndent()
    )

    write(
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

private fun KreateBuildFixture.writeRoot(aggregateBlock: String) {
    writeBuild(
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
        extraPlugins = listOf(KOVER_PLUGIN)
    )
}
