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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

/**
 * Tests for publishing native libraries as one artifact per platform.
 *
 * The effect of this feature is only visible in what actually gets published, so these tests
 * drive `publishToMavenLocal` into a throwaway repository and read the artifacts back. Asserting
 * on task wiring alone would miss the two failures that matter: natives left in the main JAR, and
 * a main artifact that silently stops being published.
 */
@DisplayName("Native publishing")
class NativePublishingFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var repositoryDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
    }

    /**
     * The platform this test runs on, derived the same way the plugin derives it.
     */
    private val hostPlatform: String = run {
        val os = System.getProperty("os.name").lowercase()
        val osId = when {
            os.contains("win") -> "windows"
            os.contains("mac") || os.contains("darwin") -> "macos"
            else -> "linux"
        }
        val arch = System.getProperty("os.arch").lowercase()
        val archId = if (arch.contains("aarch64") || arch.contains("arm64")) "aarch64" else "x86_64"
        "$osId-$archId"
    }

    /**
     * Writes a build that publishes per platform into the throwaway repository.
     *
     * The JNI toolchain is not involved: a staged binary stands in for a compiled one, which is
     * what lets these tests run without CMake and is also the mechanism a Linux-only pipeline
     * uses to publish a platform it cannot build.
     */
    private fun writeBuild(platforms: String, stagePlatforms: List<String> = listOf(hostPlatform)) {
        stagePlatforms.forEach { platform ->
            fixture.write("natives/$platform/libsample.so", "not a real binary, but a real file")
        }

        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                platform {
                    jvm {
                        jni {
                            enabled = true

                            packaging {
                                enabled = true
                                generateLoader = false

                                publishing {
                                    enabled = true
                                    platforms = listOf($platforms)
                                    stagingDirectory = layout.projectDirectory.dir("natives")
                                }
                            }
                        }
                    }
                }

                project {
                    name = "sample"
                    description = "Fixture"

                    publish {
                        enabled = true

                        repositories {
                            mavenCentral { enabled = false }
                            // Registers the publication for the library itself. Its remote
                            // repository is skipped outside a pipeline, which is what lets this
                            // test publish into a local directory instead.
                            gitlab { enabled = true }
                        }
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("maven-publish")"""),
            extra = """
                publishing {
                    repositories {
                        // A full file URI rather than a path: on Windows an absolute path like
                        // `D:/a/...` parses as a URI whose scheme is the drive letter.
                        maven { url = uri("${repositoryDir.toURI()}") }
                    }
                }
            """.trimIndent()
        )
    }

    private fun publishedModule(artifactId: String): File =
        repositoryDir.resolve("com/example/$artifactId")

    private fun jarEntries(jar: File): List<String> =
        ZipFile(jar).use { zip -> zip.entries().toList().map { it.name } }

    @Test
    @DisplayName("keeps the natives out of the main JAR")
    fun mainJarCarriesNoNatives() {
        // The whole point of the mode: no consumer receives whichever platform the library
        // happened to be built on without asking for it.
        writeBuild(""""$hostPlatform"""")

        fixture.build("publish")

        val mainJar = publishedModule("sample").walkTopDown()
            .single { it.name.endsWith(".jar") && !it.name.contains("-sources") }

        jarEntries(mainJar).none { it.startsWith("native/") } shouldBe true
    }

    @Test
    @DisplayName("publishes the main artifact alongside the platform artifact")
    fun publishesBothArtifacts() {
        // Regression guard for an ordering hazard: Kreate registers the main publication only
        // when none exists yet, so a platform publication registered too early would replace the
        // library with a bag of shared objects, and the release would still be green.
        writeBuild(""""$hostPlatform"""")

        fixture.build("publish")

        publishedModule("sample").isDirectory shouldBe true
        publishedModule("sample-$hostPlatform").isDirectory shouldBe true
    }

    @Test
    @DisplayName("the platform artifact contains that platform's library and nothing else")
    fun platformJarCarriesTheLibrary() {
        writeBuild(""""$hostPlatform"""")

        fixture.build("publish")

        val platformJar = publishedModule("sample-$hostPlatform").walkTopDown()
            .single { it.name.endsWith(".jar") }

        val entries = jarEntries(platformJar)
        entries.any { it == "native/$hostPlatform/libsample.so" } shouldBe true
        entries.none { it.endsWith(".class") } shouldBe true
    }

    @Test
    @DisplayName("the platform POM declares no dependencies")
    fun platformPomHasNoDependencies() {
        // It is a resource carrier. A dependency on the main library would point the wrong way:
        // it is the consumer that pulls both.
        writeBuild(""""$hostPlatform"""")

        fixture.build("publish")

        val pom = publishedModule("sample-$hostPlatform").walkTopDown()
            .single { it.name.endsWith(".pom") }

        pom.readText().contains("<dependencies>") shouldBe false
    }

    @Test
    @DisplayName("publishes a subset of platforms without complaining")
    fun publishesSubset() {
        // The requirement this feature exists for: infrastructure that can only build one
        // platform still produces a valid release.
        writeBuild(
            platforms = """"$hostPlatform"""",
            stagePlatforms = listOf(hostPlatform)
        )

        val result = fixture.build("publish")

        result.task(":publish")?.outcome shouldBe TaskOutcome.SUCCESS
        publishedModule("sample-$hostPlatform").isDirectory shouldBe true
    }

    @Test
    @DisplayName("fails when a selected platform has no library")
    fun failsOnSelectedButMissingPlatform() {
        // Selecting fewer platforms is fine; selecting one you cannot deliver is always an
        // accident, and one that would otherwise upload cleanly.
        val absent = if (hostPlatform == "linux-aarch64") "linux-x86_64" else "linux-aarch64"
        writeBuild(
            platforms = """"$hostPlatform", "$absent"""",
            stagePlatforms = listOf(hostPlatform)
        )

        val result = fixture.buildAndFail("kreateJniVerifyPlatforms")

        result.output shouldContain absent
        result.output shouldContain "No native library was found"
    }

    @Test
    @DisplayName("rejects an identifier that is not a platform")
    fun rejectsUnknownPlatform() {
        writeBuild(platforms = """"linux-amd64"""")

        val result = fixture.buildAndFail("tasks")

        result.output shouldContain "linux-amd64"
        result.output shouldContain "linux-x86_64"
    }

    @Test
    @DisplayName("the generated loader names the coordinate a consumer is missing")
    fun loaderNamesTheCoordinate() {
        // The message is the only thing standing between a consumer and an afternoon: with
        // separate artifacts the usual cause of a failed load is an undeclared dependency.
        fixture.write("natives/$hostPlatform/libsample.so", "stand-in for a compiled library")

        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                platform {
                    jvm {
                        jni {
                            enabled = true

                            packaging {
                                enabled = true
                                generateLoader = true

                                publishing {
                                    enabled = true
                                    platforms = listOf("$hostPlatform")
                                    stagingDirectory = layout.projectDirectory.dir("natives")
                                }
                            }
                        }
                    }
                }

                project {
                    name = "sample"
                    description = "Fixture"

                    publish {
                        enabled = true

                        repositories {
                            mavenCentral { enabled = false }
                            gitlab { enabled = true }
                        }
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("maven-publish")""")
        )

        fixture.build("kreateJniLoader")

        val loader = fixture.file("build/generated/jni/kotlin").walkTopDown()
            .single { it.name == "KreateNativeLoader.kt" }
            .readText()

        loader shouldContain "runtimeOnly"
        loader shouldContain "com.example:sample-"
        loader shouldContain "Platforms published with this version: $hostPlatform"
    }

    @Test
    @DisplayName("the command line overrides the configured selection")
    fun propertyOverridesSelection() {
        // A pipeline that gains or loses a runner should not need a commit to change what a
        // release publishes.
        val other = if (hostPlatform == "linux-aarch64") "linux-x86_64" else "linux-aarch64"
        writeBuild(
            platforms = """"$other"""",
            stagePlatforms = listOf(hostPlatform)
        )

        val result = fixture.build("publish", "-Pkreate.jni.publishPlatforms=$hostPlatform")

        result.task(":publish")?.outcome shouldBe TaskOutcome.SUCCESS
        publishedModule("sample-$hostPlatform").isDirectory shouldBe true
        publishedModule("sample-$other").exists() shouldBe false
    }
}
