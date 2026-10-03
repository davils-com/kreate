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
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory

private const val LINUX_AARCH64: String = "linux-aarch64"

private const val LINUX_X86_64: String = "linux-x86_64"

private fun hostOperatingSystem(): String {
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("win")) return "windows"
    if (os.contains("mac") || os.contains("darwin")) return "macos"
    return "linux"
}

private fun hostArchitecture(): String {
    val arch = System.getProperty("os.arch").lowercase()
    if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64"
    return "x86_64"
}

private fun hostPlatformLikeThePlugin(): String = "${hostOperatingSystem()}-${hostArchitecture()}"

private fun linuxPlatformOtherThan(platform: String): String {
    if (platform == LINUX_AARCH64) return LINUX_X86_64
    return LINUX_AARCH64
}

class NativePublishingFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    val hostPlatform = hostPlatformLikeThePlugin()

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun newRepositoryDirectory(): File = createTempDirectory(workspace.toPath(), "repository").toFile()

    fun KreateBuildFixture.writeStagedPublishingBuild(
        repositoryDir: File,
        platforms: String,
        stagePlatforms: List<String> = listOf(hostPlatform)
    ) {
        stagePlatforms.forEach { platform ->
            write("natives/$platform/libsample.so", "not a real binary, but a real file")
        }

        writeBuild(
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

    fun publishedModule(repositoryDir: File, artifactId: String): File =
        repositoryDir.resolve("com/example/$artifactId")

    fun publishedFile(repositoryDir: File, artifactId: String, isWanted: (File) -> Boolean): File {
        val moduleFiles = publishedModule(repositoryDir, artifactId).walkTopDown()
        return moduleFiles.single(isWanted)
    }

    fun jarEntries(jar: File): List<String> = ZipFile(jar).use { zip ->
        val entries = zip.entries().toList()
        entries.map { it.name }
    }

    context("Native publishing") {
        test("keeps the natives out of the main JAR") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(repositoryDir, """"$hostPlatform"""")

            fixture.build("publish")

            val mainJar = publishedFile(repositoryDir, "sample") {
                it.name.endsWith(".jar") && !it.name.contains("-sources")
            }

            jarEntries(mainJar).none { it.startsWith("native/") } shouldBe true
        }

        test("publishes the main artifact alongside the platform artifact") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(repositoryDir, """"$hostPlatform"""")

            fixture.build("publish")

            publishedModule(repositoryDir, "sample").isDirectory shouldBe true
            publishedModule(repositoryDir, "sample-$hostPlatform").isDirectory shouldBe true
        }

        test("the platform artifact contains that platform's library and nothing else") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(repositoryDir, """"$hostPlatform"""")

            fixture.build("publish")

            val platformJar = publishedFile(repositoryDir, "sample-$hostPlatform") { it.name.endsWith(".jar") }

            val entries = jarEntries(platformJar)
            entries.any { it == "native/$hostPlatform/libsample.so" } shouldBe true
            entries.none { it.endsWith(".class") } shouldBe true
        }

        test("the platform POM declares no dependencies") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(repositoryDir, """"$hostPlatform"""")

            fixture.build("publish")

            val pom = publishedFile(repositoryDir, "sample-$hostPlatform") { it.name.endsWith(".pom") }

            pom.readText().contains("<dependencies>") shouldBe false
        }

        test("publishes a subset of platforms without complaining") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(
                repositoryDir,
                platforms = """"$hostPlatform"""",
                stagePlatforms = listOf(hostPlatform)
            )

            val result = fixture.build("publish")

            result.task(":publish")?.outcome shouldBe TaskOutcome.SUCCESS
            publishedModule(repositoryDir, "sample-$hostPlatform").isDirectory shouldBe true
        }

        test("fails when a selected platform has no library") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            val absent = linuxPlatformOtherThan(hostPlatform)
            fixture.writeStagedPublishingBuild(
                repositoryDir,
                platforms = """"$hostPlatform", "$absent"""",
                stagePlatforms = listOf(hostPlatform)
            )

            val result = fixture.buildAndFail("kreateJniVerifyPlatforms")

            result.output shouldContain absent
            result.output shouldContain "No native library was found"
        }

        test("rejects an identifier that is not a platform") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            fixture.writeStagedPublishingBuild(repositoryDir, platforms = """"linux-amd64"""")

            val result = fixture.buildAndFail("tasks")

            result.output shouldContain "linux-amd64"
            result.output shouldContain "linux-x86_64"
        }

        test("the generated loader names the coordinate a consumer is missing") {
            val fixture = newFixture()
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

            val generatedSources = fixture.file("build/generated/jni/kotlin").walkTopDown()
            val loaderSource = generatedSources.single { it.name == "KreateNativeLoader.kt" }
            val loader = loaderSource.readText()

            loader shouldContain "runtimeOnly"
            loader shouldContain "com.example:sample-"
            loader shouldContain "Platforms published with this version: $hostPlatform"
        }

        test("the command line overrides the configured selection") {
            val fixture = newFixture()
            val repositoryDir = newRepositoryDirectory()
            val other = linuxPlatformOtherThan(hostPlatform)
            fixture.writeStagedPublishingBuild(
                repositoryDir,
                platforms = """"$other"""",
                stagePlatforms = listOf(hostPlatform)
            )

            val result = fixture.build("publish", "-Pkreate.jni.publishPlatforms=$hostPlatform")

            result.task(":publish")?.outcome shouldBe TaskOutcome.SUCCESS
            publishedModule(repositoryDir, "sample-$hostPlatform").isDirectory shouldBe true
            publishedModule(repositoryDir, "sample-$other").exists() shouldBe false
        }
    }
})
