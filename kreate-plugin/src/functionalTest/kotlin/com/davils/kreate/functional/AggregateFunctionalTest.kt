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
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.string.shouldContain

private const val DENIED_DEPENDENCY: String =
    """dependencies { implementation("org.apache.commons:commons-lang3:3.18.0") }"""

class AggregateFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        val settings = fixture.file("settings.gradle.kts").readText()
        fixture.write("settings.gradle.kts", settings + "\n" + """include("a", "b", "legacy")""" + "\n")
        val assurance = """
            assurance {
                enabled = true
                runOnCheck = false
                reports { formats = setOf(ReportFormat.HTML, ReportFormat.MARKDOWN) }
                aggregate {
                    enabled = true
                    members = setOf(":", ":a", ":b", ":legacy")
                }
                dependencyPolicy {
                    enabled = true
                    deny("org.apache.commons:commons-lang3", "Use the JDK.")
                }
            }
        """.trimIndent()
        fixture.writeBuild("${KreateBuildFixture.platformBlock}\n\n$assurance")
        val script = "import com.davils.kreate.assurance.ReportFormat\n\n" + fixture.file("build.gradle.kts").readText()
        fixture.write("build.gradle.kts", script)
        fixture.write("a/build.gradle.kts", script)
        fixture.write("b/build.gradle.kts", script + "\n" + DENIED_DEPENDENCY + "\n")
        fixture.write("legacy/build.gradle.kts", "plugins { id(\"org.jetbrains.kotlin.jvm\") }\n")
        fixture.write("a/src/main/kotlin/A.kt", "class A")
        fixture.write("b/src/main/kotlin/B.kt", "class B")
        return fixture
    }

    context("the aggregation") {
        test("puts every project into one report and fails with the worst status, with Isolated Projects") {
            val fixture = newBuild()

            val result = fixture.buildAndFail("kreateAssuranceAggregate", "-Dorg.gradle.unsafe.isolated-projects=true")

            result.output shouldContain "FINDINGS   :b (1 blocking)"
            result.output shouldContain "PASSED     :a (0 blocking)"
            result.output shouldContain "NOT CHECKED :legacy"
            result.output shouldContain "Kreate assurance [FINDINGS]: 1 blocking finding(s) in 3 project(s)."
            fixture.file("build/kreate/assurance/aggregate/aggregate.html").shouldExist()
            val markdown = fixture.file("build/kreate/assurance/aggregate/aggregate.md").readText()
            markdown shouldContain "| :b | FINDINGS | 1 |"
        }
    }

    context("the quality ingest") {
        test("reads the API check into the gate without blocking it") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild(
                """
                ${KreateBuildFixture.platformBlock}

                project { apiValidation { enabled = true } }

                assurance { enabled = true }
                """.trimIndent()
            )
            fixture.writeKotlin("Sample.kt", "class Sample { fun greet() = 1 }")
            fixture.build("kreateApiDump")
            fixture.writeKotlin("Sample.kt", "class Sample { fun greet() = 1\nfun wave() = 2 }")
            fixture.buildAndFail("kreateApiCheck")

            val result = fixture.build("kreateAssuranceIngest", "kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
            fixture.file("build/kreate/assurance/documents/quality.json").readText() shouldContain "API_DUMP_OUTDATED"
        }
    }
})
