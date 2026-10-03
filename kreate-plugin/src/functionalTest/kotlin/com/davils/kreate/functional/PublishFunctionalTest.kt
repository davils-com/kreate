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
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class PublishFunctionalTest : FunSpec({

    val workspace = tempdir()

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings(projectName = "sample-library")
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun publishBlock(name: String = "TestRegistry") = """
        project {
            description = "Fixture"

            publish {
                enabled = true
                website = "https://example.com"

                repositories {
                    gitlab {
                        enabled = true
                        name = "$name"
                        tokenEnv = "KREATE_TEST_TOKEN"
                        projectIdEnv = "KREATE_TEST_PROJECT_ID"
                        apiUrlEnv = "KREATE_TEST_API_URL"
                    }
                }
            }
        }
    """.trimIndent()

    fun jvmKreateBlock() = """
        ${KreateBuildFixture.platformBlock}

        ${publishBlock()}
    """.trimIndent()

    fun KreateBuildFixture.withCiEnvironment() {
        withEnvironment("KREATE_TEST_TOKEN", "test-job-token")
        withEnvironment("KREATE_TEST_PROJECT_ID", "4711")
        withEnvironment("KREATE_TEST_API_URL", "https://gitlab.example.com/api/v4")
    }

    context("GitLab publishing") {
        test("wires publish to a real upload task") {
            val fixture = newFixture()
            fixture.withCiEnvironment()
            fixture.writeBuild(jvmKreateBlock(), extraPlugins = listOf("""`maven-publish`"""))

            val dryRunTaskGraph = fixture.build("publish", "--dry-run")

            dryRunTaskGraph.output shouldContain ":publishMavenPublicationToTestRegistryRepository"
        }

        test("registers the publication outside CI as well") {
            val fixture = newFixture()
            fixture.writeBuild(jvmKreateBlock(), extraPlugins = listOf("""`maven-publish`"""))

            val result = fixture.build("generatePomFileForMavenPublication")

            result.task(":generatePomFileForMavenPublication")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "No CI job token found in KREATE_TEST_TOKEN"

            val pom = fixture.file("build/publications/maven/pom-default.xml").readText()
            pom shouldContain "<artifactId>sample-library</artifactId>"
            pom shouldContain "<description>Fixture</description>"
            pom shouldContain "<url>https://example.com</url>"
        }

        test("publishes a java-platform as a BOM") {
            val fixture = newFixture()
            fixture.withCiEnvironment()
            fixture.write(
                "build.gradle.kts",
                """
                plugins {
                    `java-platform`
                    `maven-publish`
                    id("com.davils.kreate")
                }

                group = "com.example"

                kreate {
                    ${publishBlock(name = "BomRegistry")}
                }
                """.trimIndent()
            )

            val result = fixture.build("publish", "--dry-run")

            result.output shouldContain ":publishMavenPublicationToBomRegistryRepository"
        }

        test("fails with a readable message when the registry URL is incomplete") {
            val fixture = newFixture()
            fixture.withEnvironment("KREATE_TEST_TOKEN", "test-job-token")
            fixture.writeBuild(jvmKreateBlock(), extraPlugins = listOf("""`maven-publish`"""))

            val resultWithTokenButNoProjectId = fixture.buildAndFail("publish", "--dry-run")

            resultWithTokenButNoProjectId.output shouldContain "KREATE_TEST_PROJECT_ID = unset"
        }
    }
})
