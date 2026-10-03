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
import java.io.File
import java.util.Properties

class GitlabRegistryFunctionalTest : FunSpec({

    val workspace = tempdir()

    val endpoint = "https://gitlab.example.com/api/v4/groups/42/-/packages/maven"

    val reportTask = """
        val described = repositories.filterIsInstance<MavenArtifactRepository>().joinToString("\n") { repo ->
            val credentials = repo.getCredentials(HttpHeaderCredentials::class.java)
            "repo=" + repo.name +
                " url=" + repo.url +
                " header=" + credentials.name +
                " token=" + credentials.value +
                " auth=" + repo.authentication.joinToString(",") { it.name }
        }

        tasks.register("reportRepository") {
            // Copied into a local first: a lambda reading the script-level `val` directly would
            // capture the script object, which the configuration cache cannot serialise.
            val message = described
            doLast { println(message) }
        }
    """.trimIndent()

    fun newFixture(): KreateBuildFixture = KreateBuildFixture.createIn(workspace)

    fun KreateBuildFixture.writeProjectBuild(block: String) {
        writeSettings()
        write(
            "build.gradle.kts",
            """
            import com.davils.kreate.publish.gitlabPackageRegistry
            import org.gradle.api.artifacts.repositories.MavenArtifactRepository
            import org.gradle.api.credentials.HttpHeaderCredentials

            plugins {
                id("org.jetbrains.kotlin.jvm")
                id("com.davils.kreate")
            }

            group = "com.example"

            repositories {
                $block
            }

            $reportTask
            """.trimIndent()
        )
    }

    fun pluginClasspathLiteral(): String {
        val metadata = javaClass.classLoader.getResourceAsStream("plugin-under-test-metadata.properties")
            ?: error("TestKit did not generate plugin-under-test-metadata.properties")

        val properties = Properties().apply { metadata.use { load(it) } }
        val classpath = properties.getProperty("implementation-classpath")
            ?: error("plugin-under-test-metadata.properties has no implementation-classpath")

        return classpath.split(File.pathSeparator)
            .joinToString(", ") { "\"${File(it).invariantSeparatorsPath}\"" }
    }

    context("GitLab package registry") {
        test("uses the job token as Job-Token inside a pipeline") {
            val fixture = newFixture()
            fixture.writeProjectBuild("""gitlabPackageRegistry(providers) { url = "$endpoint" }""")

            val result = fixture.buildWithEnvironment(
                mapOf("CI_JOB_TOKEN" to "pipeline-token", "GITLAB_TOKEN" to "personal-token"),
                "reportRepository"
            )

            result.output shouldContain "header=Job-Token"
            result.output shouldContain "token=pipeline-token"
            result.output shouldContain "auth=header"
        }

        test("falls back to a personal token as Private-Token outside one") {
            val fixture = newFixture()
            fixture.writeProjectBuild("""gitlabPackageRegistry(providers) { url = "$endpoint" }""")

            val result = fixture.buildWithEnvironment(
                mapOf("GITLAB_TOKEN" to "personal-token"),
                "reportRepository"
            )

            result.output shouldContain "header=Private-Token"
            result.output shouldContain "token=personal-token"
        }

        test("prefers the Gradle property over the environment variable") {
            val fixture = newFixture()
            fixture.writeProjectBuild("""gitlabPackageRegistry(providers) { url = "$endpoint" }""")

            val result = fixture.buildWithEnvironment(
                mapOf("GITLAB_TOKEN" to "from-environment"),
                "reportRepository",
                "-PgitlabToken=from-property"
            )

            result.output shouldContain "token=from-property"
        }

        test("honours custom repository, property and header names") {
            val fixture = newFixture()
            fixture.writeProjectBuild(
                """
                gitlabPackageRegistry(providers) {
                    url = "$endpoint"
                    name = "Internal"
                    tokenProperty = "internalToken"
                    tokenHeader = "Deploy-Token"
                }
                """.trimIndent()
            )

            val result = fixture.buildWithEnvironment(
                emptyMap(),
                "reportRepository",
                "-PinternalToken=deploy"
            )

            result.output shouldContain "repo=Internal"
            result.output shouldContain "header=Deploy-Token"
            result.output shouldContain "token=deploy"
        }

        test("fails with an actionable message when no URL is given") {
            val fixture = newFixture()
            fixture.writeProjectBuild("""gitlabPackageRegistry(providers) { name = "Internal" }""")

            val result = fixture.buildAndFail("reportRepository")

            result.output shouldContain "declared without a URL"
            result.output shouldContain "packages/maven"
        }

        test("applies the content filter it was given") {
            val fixture = newFixture()
            fixture.writeProjectBuild(
                """
                gitlabPackageRegistry(providers) {
                    url = "$endpoint"
                    content { includeGroup("com.example") }
                }
                """.trimIndent()
            )

            val result = fixture.buildWithEnvironment(
                mapOf("GITLAB_TOKEN" to "personal-token"),
                "reportRepository"
            )

            result.task(":reportRepository")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("can be declared in the settings file") {
            val fixture = newFixture()
            fixture.write(
                "settings.gradle.kts",
                """
                import com.davils.kreate.publish.gitlabPackageRegistry
                import org.gradle.api.artifacts.repositories.MavenArtifactRepository
                import org.gradle.api.credentials.HttpHeaderCredentials

                buildscript {
                    dependencies {
                        classpath(files(${pluginClasspathLiteral()}))
                    }
                }

                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()

                        gitlabPackageRegistry(providers) {
                            url = "$endpoint"
                            content { includeGroup("com.example") }
                        }
                    }
                }

                // Read straight back out of the settings model. Repositories declared here never
                // reach `project.repositories`, so a task in the build script would report nothing
                // and the test would pass against a helper that did nothing at all.
                val described = dependencyResolutionManagement.repositories
                    .filterIsInstance<MavenArtifactRepository>()
                    .joinToString("\n") { repo ->
                        val credentials = repo.getCredentials(HttpHeaderCredentials::class.java)
                        "repo=" + repo.name +
                            " url=" + repo.url +
                            " header=" + credentials.name +
                            " token=" + credentials.value
                    }

                settingsDir.resolve("repositories.txt").writeText(described)

                rootProject.name = "sample"
                """.trimIndent()
            )

            fixture.write(
                "build.gradle.kts",
                """
                plugins {
                    id("org.jetbrains.kotlin.jvm")
                }
                """.trimIndent()
            )

            val result = fixture.buildWithEnvironment(
                mapOf("GITLAB_TOKEN" to "personal-token"),
                "tasks"
            )

            result.task(":tasks")?.outcome shouldBe TaskOutcome.SUCCESS

            val described = fixture.file("repositories.txt").readText()
            described shouldContain "url=$endpoint"
            described shouldContain "header=Private-Token"
            described shouldContain "token=personal-token"
        }
    }
})
