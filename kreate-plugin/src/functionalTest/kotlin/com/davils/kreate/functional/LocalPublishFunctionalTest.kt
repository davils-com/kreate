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

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the producer half of the local development workflow.
 *
 * Every assertion here is about the moment a developer types `kreateLocalPublish` and about the
 * refusals that keep that from happening anywhere it should not. The consumer half — substituting
 * the result into another checkout — is covered separately, because it needs the settings plugin.
 */
@DisplayName("kreateLocalPublish")
class LocalPublishFunctionalTest {

    @TempDir
    lateinit var projectDirectory: File

    private fun producer(
        version: String = "1.4.0",
        extraPlugins: List<String> = emptyList(),
        extra: String = ""
    ): KreateBuildFixture {
        val fixture = KreateBuildFixture(projectDirectory)
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

    @Nested
    @DisplayName("publishing")
    inner class Publishing {

        @Test
        @DisplayName("installs the build at a snapshot version without anyone naming one")
        fun installsSnapshot() {
            val fixture = producer(version = "1.4.0")

            val result = fixture.build("kreateLocalPublish")

            result.task(":kreateLocalPublish")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.mavenRepository.resolve("com/example/producer/1.4.0-SNAPSHOT")
                .isDirectory shouldBe true
        }

        @Test
        @DisplayName("leaves the released version alone, so a local build cannot shadow it")
        fun doesNotTouchTheRelease() {
            val fixture = producer(version = "1.4.0")

            fixture.build("kreateLocalPublish")

            // The suffix is the whole safety story: `1.4.0` and `1.4.0-SNAPSHOT` are different
            // coordinates, and the consumer declares mavenLocal with `snapshotsOnly()`.
            fixture.mavenRepository.resolve("com/example/producer/1.4.0").exists() shouldBe false
        }

        @Test
        @DisplayName("records the coordinates it installed")
        fun recordsCoordinates() {
            val fixture = producer()

            fixture.build("kreateLocalPublish")

            val record = fixture.stateDirectory.resolve("com.example.producer.properties")
            record.isFile shouldBe true

            val contents = record.readText()
            contents shouldContain "version=1.4.0-SNAPSHOT"
            contents shouldContain "producer"
        }

        @Test
        @DisplayName("does not suffix the version of an ordinary build")
        fun ordinaryBuildIsUnaffected() {
            val fixture = producer(version = "1.4.0")

            val result = fixture.build("publishToMavenLocal")

            result.task(":publishToMavenLocal")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.mavenRepository.resolve("com/example/producer/1.4.0").isDirectory shouldBe true
            fixture.stateDirectory.exists() shouldBe false
        }

        @Test
        @DisplayName("invalidates the configuration cache, so the next build sees the new build")
        fun publishingInvalidatesTheCache() {
            val fixture = producer()

            fixture.build("kreateLocalPublish")
            val afterPublish = fixture.build("kreateLocalPublish")

            // This is the ValueSource earning its place. Republishing the same snapshot version
            // with changed content is the normal inner loop, and a consumer replaying a cached
            // configuration would keep resolving the previous one without a word.
            afterPublish.output shouldContain "configuration cache cannot be reused"
            afterPublish.output shouldContain "LocalWorkspaceSource"
        }

        @Test
        @DisplayName("leaves the configuration cache reusable when the local state is unchanged")
        fun unchangedStateReusesTheCache() {
            val fixture = producer()
            fixture.build("kreateLocalPublish")

            fixture.build("kreateLocalStatus")
            val second = fixture.build("kreateLocalStatus")

            second.output shouldContain "Reusing configuration cache"
        }
    }

    @Nested
    @DisplayName("refusals")
    inner class Refusals {

        @Test
        @DisplayName("will not run in CI, where a local publication has no meaning")
        fun refusesInCi() {
            val fixture = producer()
            fixture.withEnvironment("GITLAB_CI", "true")

            val result = fixture.buildAndFail("kreateLocalPublish")

            result.output shouldContain "must not run in CI"
        }

        @Test
        @DisplayName("says so when the build publishes nothing at all")
        fun refusesWithoutPublications() {
            val fixture = KreateBuildFixture(projectDirectory)
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

    @Nested
    @DisplayName("kreateLocalStatus")
    inner class Status {

        @Test
        @DisplayName("says nothing is published before the first publish")
        fun emptyStatus() {
            val fixture = producer()

            val result = fixture.build("kreateLocalStatus")

            result.output shouldContain "Nothing is published locally"
        }

        @Test
        @DisplayName("lists the publication and the version it will substitute")
        fun listsPublication() {
            val fixture = producer(version = "2.0.0")
            fixture.build("kreateLocalPublish")

            val result = fixture.build("kreateLocalStatus")

            result.output shouldContain "producer"
            result.output shouldContain "2.0.0-SNAPSHOT"
            result.output shouldContain "Local mode is ON"
        }
    }

    @Nested
    @DisplayName("kreateLocalClean")
    inner class Clean {

        @Test
        @DisplayName("removes exactly what was published, and the record with it")
        fun removesPublication() {
            val fixture = producer(version = "1.4.0")
            fixture.build("kreateLocalPublish")

            fixture.build("kreateLocalClean")

            fixture.mavenRepository.resolve("com/example/producer/1.4.0-SNAPSHOT")
                .exists() shouldBe false
            fixture.stateDirectory.resolve("com.example.producer.properties")
                .exists() shouldBe false
        }

        @Test
        @DisplayName("leaves a release in the same repository untouched")
        fun leavesReleasesAlone() {
            val fixture = producer(version = "1.4.0")
            fixture.build("publishToMavenLocal")
            fixture.build("kreateLocalPublish")

            fixture.build("kreateLocalClean")

            // Something other than this feature put the release there — most likely the
            // developer, deliberately. Removing it is not this task's business.
            fixture.mavenRepository.resolve("com/example/producer/1.4.0").isDirectory shouldBe true
        }

        @Test
        @DisplayName("is a no-op when nothing is published")
        fun nothingToClean() {
            val fixture = producer()

            val result = fixture.build("kreateLocalClean")

            result.output shouldContain "Nothing to clean"
        }
    }

    @Nested
    @DisplayName("guards that hold once local mode is on")
    inner class Guards {

        @Test
        @DisplayName("refuses to write a lock file that would pin a local snapshot")
        fun refusesToWriteLocks() {
            val fixture = producer()
            fixture.write(
                "build.gradle.kts",
                fixture.file("build.gradle.kts").readText().replace(
                    "publish {",
                    """
                    dependencyLocking {
                        enabled = true
                    }
                    publish {
                    """.trimIndent()
                )
            )
            fixture.build("kreateLocalPublish")

            val result = fixture.buildAndFail("kreateResolveAndLockAll", "--write-locks")

            // The single most valuable rail in the feature: a lock file pinning
            // `1.4.0-SNAPSHOT` looks plausible in review and breaks every pipeline.
            result.output shouldContain "Refusing to write lock files"
            result.output shouldContain "com.example:producer:1.4.0-SNAPSHOT"
        }

        @Test
        @DisplayName("turns dependency locking off rather than failing every build")
        fun deactivatesLocking() {
            val fixture = producer()
            fixture.write(
                "build.gradle.kts",
                fixture.file("build.gradle.kts").readText().replace(
                    "publish {",
                    """
                    dependencyLocking {
                        enabled = true
                    }
                    publish {
                    """.trimIndent()
                )
            )
            fixture.build("kreateLocalPublish")

            val result = fixture.build("kreateLocalStatus")

            result.output shouldContain "dependency locking is off"
        }

        @Test
        @DisplayName("refuses to push a build made against local artefacts to a shared registry")
        fun refusesRemotePublish() {
            // `maven-publish` is declared explicitly so that the `publishing { }` accessor exists
            // while the script compiles. Kreate applies the same plugin, but not until
            // `afterEvaluate`, which is far too late for a type-safe accessor.
            //
            // A file backed repository stands in for the GitLab registry: what is under test is
            // that the task is blocked, not where it would have uploaded to.
            val fixture = producer(
                extraPlugins = listOf("""id("maven-publish")"""),
                extra = """
                    publishing {
                        repositories {
                            maven {
                                name = "Shared"
                                url = uri(layout.buildDirectory.dir("shared-registry"))
                            }
                        }
                    }
                """.trimIndent()
            )
            fixture.build("kreateLocalPublish")

            val result = fixture.buildAndFail("publishAllPublicationsToSharedRepository")

            result.output shouldContain "Refusing to publish to a remote repository"
            result.output shouldContain "producer:1.4.0-SNAPSHOT"
        }

        @Test
        @DisplayName("still allows the local publication itself, which is the point")
        fun localPublishStillWorks() {
            val fixture = producer()
            fixture.build("kreateLocalPublish")

            // `PublishToMavenLocal` is a sibling of `PublishToMavenRepository`, not a subclass,
            // so the guard above does not reach it. Widening the guard would break the feature.
            val result = fixture.build("publishToMavenLocal")

            result.task(":publishToMavenLocal")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }

    @Nested
    @DisplayName("the task listing")
    inner class Listing {

        @Test
        @DisplayName("groups the tasks so a developer can find them without documentation")
        fun tasksAreDiscoverable() {
            val fixture = producer()

            val result = fixture.build("tasks", "--group", "kreate local")

            listOf("kreateLocalPublish", "kreateLocalStatus", "kreateLocalClean").forEach { task ->
                result.output.lines().map { it.substringBefore(" - ").trim() } shouldContain task
            }
        }
    }
}
