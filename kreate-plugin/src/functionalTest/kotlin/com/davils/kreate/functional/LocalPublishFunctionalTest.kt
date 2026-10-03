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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class LocalPublishFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun producer(
        version: String = "1.4.0",
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings("producer")
        fixture.writeBuild(
            extraPlugins = extraPlugins,
            extra = extra,
            kreateBlock = """
                project {
                    name = "producer"
                    version {
                        environment = "PRODUCER_VERSION"
                        property = "producer.version"
                    }
                    publish {
                        enabled = true
                        // The same shape every Davils library uses. Without a target Kreate
                        // registers no publication at all, and `publishToMavenLocal` would
                        // quietly have nothing to install.
                        repositories {
                            gitlab {
                                enabled = true
                            }
                        }
                    }
                }
                ${KreateBuildFixture.platformBlock}
            """.trimIndent()
        )
        fixture.write("gradle.properties", "producer.version=$version")
        fixture.writeKotlin("Sample.kt", "class Sample")
        return fixture
    }

    fun KreateBuildFixture.enableDependencyLocking() {
        val script = file("build.gradle.kts").readText()
        val withLocking = script.replace(
            "publish {",
            """
            dependencyLocking {
                enabled = true
            }
            publish {
            """.trimIndent()
        )
        write("build.gradle.kts", withLocking)
    }

    context("kreateLocalPublish") {
        context("publishing") {
            test("installs the build at a snapshot version without anyone naming one") {
                val fixture = producer(version = "1.4.0")

                val result = fixture.build("kreateLocalPublish")

                result.task(":kreateLocalPublish")?.outcome shouldBe TaskOutcome.SUCCESS
                fixture.mavenRepository.resolve("com/example/producer/1.4.0-SNAPSHOT")
                    .isDirectory shouldBe true
            }

            test("leaves the released version alone, so a local build cannot shadow it") {
                val fixture = producer(version = "1.4.0")

                fixture.build("kreateLocalPublish")

                val release = fixture.mavenRepository.resolve("com/example/producer/1.4.0")
                release.exists() shouldBe false
            }

            test("records the coordinates it installed") {
                val fixture = producer()

                fixture.build("kreateLocalPublish")

                val record = fixture.stateDirectory.resolve("com.example.producer.properties")
                record.isFile shouldBe true

                val contents = record.readText()
                contents shouldContain "version=1.4.0-SNAPSHOT"
                contents shouldContain "producer"
            }

            test("does not suffix the version of an ordinary build") {
                val fixture = producer(version = "1.4.0")

                val result = fixture.build("publishToMavenLocal")

                result.task(":publishToMavenLocal")?.outcome shouldBe TaskOutcome.SUCCESS
                fixture.mavenRepository.resolve("com/example/producer/1.4.0").isDirectory shouldBe true
                fixture.stateDirectory.exists() shouldBe false
            }

            test("invalidates the configuration cache, so the next build sees the new build") {
                val fixture = producer()

                fixture.build("kreateLocalPublish")
                val afterPublish = fixture.build("kreateLocalPublish")

                afterPublish.output shouldContain "configuration cache cannot be reused"
                afterPublish.output shouldContain "LocalWorkspaceSource"
            }

            test("leaves the configuration cache reusable when the local state is unchanged") {
                val fixture = producer()
                fixture.build("kreateLocalPublish")

                fixture.build("kreateLocalStatus")
                val second = fixture.build("kreateLocalStatus")

                second.output shouldContain "Reusing configuration cache"
            }
        }

        context("refusals") {
            test("will not run in CI, where a local publication has no meaning") {
                val fixture = producer()
                fixture.withEnvironment("GITLAB_CI", "true")

                val result = fixture.buildAndFail("kreateLocalPublish")

                result.output shouldContain "must not run in CI"
            }

            test("says so when the build publishes nothing at all") {
                val fixture = KreateBuildFixture.createIn(workspace)
                fixture.writeSettings("silent")
                fixture.writeBuild(
                    kreateBlock = """
                        project {
                            name = "silent"
                            version { property = "silent.version" }
                        }
                        ${KreateBuildFixture.platformBlock}
                    """.trimIndent()
                )
                fixture.write("gradle.properties", "silent.version=1.0.0")
                fixture.writeKotlin("Sample.kt", "class Sample")

                val result = fixture.buildAndFail("kreateLocalPublish")

                result.output shouldContain "found no Maven publications"
            }
        }

        context("kreateLocalStatus") {
            test("says nothing is published before the first publish") {
                val fixture = producer()

                val result = fixture.build("kreateLocalStatus")

                result.output shouldContain "Nothing is published locally"
            }

            test("lists the publication and the version it will substitute") {
                val fixture = producer(version = "2.0.0")
                fixture.build("kreateLocalPublish")

                val result = fixture.build("kreateLocalStatus")

                result.output shouldContain "producer"
                result.output shouldContain "2.0.0-SNAPSHOT"
                result.output shouldContain "Local mode is ON"
            }
        }

        context("kreateLocalClean") {
            test("removes exactly what was published, and the record with it") {
                val fixture = producer(version = "1.4.0")
                fixture.build("kreateLocalPublish")

                fixture.build("kreateLocalClean")

                fixture.mavenRepository.resolve("com/example/producer/1.4.0-SNAPSHOT")
                    .exists() shouldBe false
                fixture.stateDirectory.resolve("com.example.producer.properties")
                    .exists() shouldBe false
            }

            test("leaves a release in the same repository untouched") {
                val fixture = producer(version = "1.4.0")
                fixture.build("publishToMavenLocal")
                fixture.build("kreateLocalPublish")

                fixture.build("kreateLocalClean")

                val releasePutThereByTheDeveloper = fixture.mavenRepository.resolve("com/example/producer/1.4.0")
                releasePutThereByTheDeveloper.isDirectory shouldBe true
            }

            test("is a no-op when nothing is published") {
                val fixture = producer()

                val result = fixture.build("kreateLocalClean")

                result.output shouldContain "Nothing to clean"
            }
        }

        context("guards that hold once local mode is on") {
            test("refuses to write a lock file that would pin a local snapshot") {
                val fixture = producer()
                fixture.enableDependencyLocking()
                fixture.build("kreateLocalPublish")

                val result = fixture.buildAndFail("kreateResolveAndLockAll", "--write-locks")

                result.output shouldContain "Refusing to write lock files"
                result.output shouldContain "com.example:producer:1.4.0-SNAPSHOT"
            }

            test("turns dependency locking off rather than failing every build") {
                val fixture = producer()
                fixture.enableDependencyLocking()
                fixture.build("kreateLocalPublish")

                val result = fixture.build("kreateLocalStatus")

                result.output shouldContain "dependency locking is off"
            }

            test("refuses to push a build made against local artefacts to a shared registry") {
                val mavenPublishForTheTypeSafeAccessor = listOf("""id("maven-publish")""")
                val fileBackedStandInForTheSharedRegistry = """
                    publishing {
                        repositories {
                            maven {
                                name = "Shared"
                                url = uri(layout.buildDirectory.dir("shared-registry"))
                            }
                        }
                    }
                """.trimIndent()
                val fixture = producer(
                    extraPlugins = mavenPublishForTheTypeSafeAccessor,
                    extra = fileBackedStandInForTheSharedRegistry
                )
                fixture.build("kreateLocalPublish")

                val result = fixture.buildAndFail("publishAllPublicationsToSharedRepository")

                result.output shouldContain "Refusing to publish to a remote repository"
                result.output shouldContain "producer:1.4.0-SNAPSHOT"
            }

            test("still allows the local publication itself, which is the point") {
                val fixture = producer()
                fixture.build("kreateLocalPublish")

                val result = fixture.build("publishToMavenLocal")

                result.task(":publishToMavenLocal")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        context("the task listing") {
            test("groups the tasks so a developer can find them without documentation") {
                val fixture = producer()

                val result = fixture.build("tasks", "--group", "kreate local")

                val listedTasks = result.output.lines().map { it.substringBefore(" - ").trim() }
                listOf("kreateLocalPublish", "kreateLocalStatus", "kreateLocalClean").forEach { task ->
                    listedTasks shouldContain task
                }
            }
        }
    }
})
