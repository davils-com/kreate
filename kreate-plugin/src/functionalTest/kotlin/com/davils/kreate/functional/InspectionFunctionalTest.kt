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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

private const val DEPENDENCY: String = "org.apache.commons:commons-lang3:3.14.0"

private const val DOCUMENT: String = "build/kreate/assurance/documents/inspection.json"

class InspectionFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(inspectionBlock: String = "", dependency: String = DEPENDENCY): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    inspection {
                        enabled = true
                        $inspectionBlock
                    }
                }
            """.trimIndent(),
            extra = """dependencies { implementation("$dependency") }"""
        )
        return fixture
    }

    context("the dependency inspection") {
        test("is not there unless it is enabled") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild(
                """
                ${KreateBuildFixture.platformBlock}

                assurance { enabled = true }
                """.trimIndent()
            )

            val tasks = fixture.build("tasks", "--all")

            tasks.output.contains("kreateDependencyInspect") shouldBe false
        }

        test("reads the real jars of the resolved dependencies") {
            val fixture = newBuild()

            val result = fixture.build("kreateDependencyInspect")

            result.output shouldContain "Inspected "
            fixture.file(DOCUMENT).shouldExist()
        }

        test("does not block the gate by default") {
            val fixture = newBuild("failScore = 1")

            val result = fixture.build("kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
        }

        test("blocks the gate when asked to") {
            val fixture = newBuild(
                """
                failScore = 1
                failOnRisk = true
                """.trimIndent()
            )

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [FINDINGS]"
            fixture.file(DOCUMENT).readText() shouldContain "RISK_SCORE"
        }

        test("an ignore rule accepts a module") {
            val fixture = newBuild(
                """
                failScore = 1
                failOnRisk = true
                """.trimIndent()
            )
            val script = fixture.file("build.gradle.kts")
            val accepted = script.readText().replace(
                "assurance {",
                """
                assurance {
                    ignore("RISK_SCORE") { reason = "Reviewed." }
                """.trimIndent()
            )
            fixture.write("build.gradle.kts", accepted)

            fixture.build("kreateAssurance").output shouldContain "Kreate assurance [PASSED]"
        }

        test("reports a dependency without artifact as incomplete") {
            val fixture = newBuild(dependency = "org.example.kreate:missing:1.0")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }

        test("reads the POM of a module that moved away and has no jar of its own") {
            val fixture = newBuild(dependency = "org.example.kreate:moved:1.0")
            val script = fixture.file("build.gradle.kts")
            val withRepository = """
                repositories { maven { url = uri("local-repository") } }

            """.trimIndent() + script.readText()
            fixture.write("build.gradle.kts", withRepository)
            fixture.write(
                "local-repository/org/example/kreate/moved/1.0/moved-1.0.pom",
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example.kreate</groupId>
                  <artifactId>moved</artifactId>
                  <version>1.0</version>
                  <distributionManagement>
                    <relocation><artifactId>target</artifactId></relocation>
                  </distributionManagement>
                </project>
                """.trimIndent()
            )
            fixture.write(
                "local-repository/org/example/kreate/target/1.0/target-1.0.pom",
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example.kreate</groupId>
                  <artifactId>target</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <repositories>
                    <repository><id>evil</id><url>https://repo.evil.example/maven</url></repository>
                  </repositories>
                </project>
                """.trimIndent()
            )

            fixture.build("kreateDependencyInspect")

            val document = fixture.file(DOCUMENT).readText()
            document shouldContain "POM_RELOCATED"
            document shouldContain "POM_REPOSITORY"
            document shouldContain "repo.evil.example"
        }

        test("is up to date on a second run, and the configuration cache applies") {
            val fixture = newBuild()
            fixture.build("kreateDependencyInspect")

            val second = fixture.build("kreateDependencyInspect")

            second.task(":kreateDependencyInspect")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            second.output shouldContain "Reusing configuration cache"
        }
    }
})
