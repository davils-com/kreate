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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

private const val RELEASE: String = "1.0.0"

private const val SNAPSHOT: String = "1.0.0-SNAPSHOT"

private const val STAGE_ARGUMENT: String = "-Pkreate.jni.stage=stage"

private const val PUBLISH_TO_DAVILS: String = "publishAllPublicationsToDavilsRepository"

private val ALL_PLATFORMS: List<String> = listOf(
    "linux-x86_64",
    "linux-aarch64",
    "windows-x86_64",
    "windows-aarch64",
    "macos-x86_64",
    "macos-aarch64"
)

private val PLATFORMS_ARGUMENT: String = "-Pkreate.jni.publishPlatforms=${ALL_PLATFORMS.joinToString(",")}"

private val LIBRARY_FILES: Map<String, String> = mapOf(
    "linux" to "lib%s.so",
    "windows" to "%s.dll",
    "macos" to "lib%s.dylib"
)

private fun libraryFileOf(platform: String, library: String = "sample"): String {
    val pattern = LIBRARY_FILES.getValue(platform.substringBefore('-'))
    return pattern.format(library)
}

private fun sha256Of(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(file.readBytes())
    return hash.joinToString("") { byte -> "%02x".format(byte) }
}

private fun git(directory: File, vararg arguments: String): String {
    val command = listOf("git", "-c", "user.name=Kreate", "-c", "user.email=kreate@example.com") + arguments
    val builder = ProcessBuilder(command).directory(directory)
    val process = builder.redirectErrorStream(true).start()
    val reader = process.inputStream.bufferedReader()
    val output = reader.readText()
    check(process.waitFor() == 0) { "git ${arguments.joinToString(" ")} failed: $output" }
    return output.trim()
}

class NativeReleaseFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun newFixture(withJvmSource: Boolean = true): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        if (!withJvmSource) return fixture
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun KreateBuildFixture.stage(
        platform: String,
        version: String = RELEASE,
        revision: String? = null,
        artifact: String = "sample",
        library: String = "sample"
    ): File {
        val libraryPath = "stage/$artifact/$platform/${libraryFileOf(platform, library)}"
        val libraryFile = write(libraryPath, "native code of $platform")
        val lines = listOfNotNull(
            "format=1",
            "platform=$platform",
            "group=com.example",
            "artifact=$artifact",
            "version=$version",
            revision?.let { value -> "revision=$value" },
            "sha256.${libraryFile.name}=${sha256Of(libraryFile)}"
        )
        write("stage/$artifact/$platform/native.properties", lines.joinToString(separator = "\n"))
        return libraryFile
    }

    fun KreateBuildFixture.stageAll(version: String = RELEASE, revision: String? = null) {
        ALL_PLATFORMS.forEach { platform -> stage(platform, version, revision) }
    }

    fun jniBlock(nameOverride: String = """nameOverride = "sample""""): String = """
        platform {
            jvm {
                jni {
                    enabled = true
                    $nameOverride

                    packaging {
                        enabled = true

                        publishing {
                            enabled = true
                        }
                    }
                }
            }
        }
    """.trimIndent()

    fun KreateBuildFixture.writeMavenTargetBuild(targetExtras: String = "", named: Boolean = true) {
        val name = """name = "sample"""".takeIf { named }.orEmpty()
        val nameOverride = """nameOverride = "sample"""".takeIf { named }.orEmpty()
        val releases = file("repository/releases").toURI()
        val snapshots = file("repository/snapshots").toURI()
        writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                ${jniBlock(nameOverride)}

                project {
                    $name
                    description = "Fixture"

                    publish {
                        enabled = true
                        website = "https://example.com"

                        repositories {
                            maven {
                                name = "Davils"
                                usernameEnv = "KREATE_TEST_MAVEN_NAME"
                                passwordEnv = "KREATE_TEST_MAVEN_SECRET"
                                releases = "$releases"
                                snapshots = "$snapshots"
                                $targetExtras
                            }
                        }
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""`maven-publish`""")
        )
    }

    fun KreateBuildFixture.releasedModule(artifactId: String): File =
        file("repository/releases/com/example/$artifactId/$RELEASE")

    fun jarEntries(jar: File): List<String> = ZipFile(jar).use { zip ->
        val entries = zip.entries().toList()
        entries.map { entry -> entry.name }
    }

    context("Releasing natives collected from several runners") {
        test("publishes the library and all six platform artifacts from the stage") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.writeMavenTargetBuild()

            val result = fixture.build(PUBLISH_TO_DAVILS, STAGE_ARGUMENT, PLATFORMS_ARGUMENT, "-Pversion=$RELEASE")

            result.task(":$PUBLISH_TO_DAVILS")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.releasedModule("sample").isDirectory shouldBe true
            ALL_PLATFORMS.forEach { platform ->
                val jar = fixture.releasedModule("sample-$platform").resolve("sample-$platform-$RELEASE.jar")
                jarEntries(jar) shouldContainExactlyInAnyOrder listOf(
                    "META-INF/",
                    "META-INF/MANIFEST.MF",
                    "native/",
                    "native/$platform/",
                    "native/$platform/${libraryFileOf(platform)}",
                    "native/digests.properties"
                )
                val pom = fixture.releasedModule("sample-$platform").resolve("sample-$platform-$RELEASE.pom")
                pom.readText() shouldNotContain "<dependencies>"
            }
        }

        test("names the platform artifacts after the library's artifact, not after its native library") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings(projectName = "pipe-core")
            fixture.writeKotlin(
                "com/example/Pipe.kt",
                """
                package com.example

                class Pipe
                """.trimIndent()
            )
            val platforms = listOf("linux-x86_64", "windows-x86_64")
            platforms.forEach { platform -> fixture.stage(platform, artifact = "pipe-core", library = "pipe_core") }
            fixture.writeMavenTargetBuild(named = false)

            val selection = "-Pkreate.jni.publishPlatforms=${platforms.joinToString(",")}"
            fixture.build(PUBLISH_TO_DAVILS, STAGE_ARGUMENT, selection, "-Pversion=$RELEASE")

            fixture.releasedModule("pipe-core").isDirectory shouldBe true
            platforms.forEach { platform ->
                val module = fixture.releasedModule("pipe-core-$platform")
                val jar = module.resolve("pipe-core-$platform-$RELEASE.jar")
                jarEntries(jar) shouldContain "native/$platform/${libraryFileOf(platform, "pipe_core")}"
                fixture.releasedModule("pipe_core-$platform").exists() shouldBe false
            }
        }

        test("does not build the native library of the publishing machine") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.writeMavenTargetBuild()

            val result = fixture.build(PUBLISH_TO_DAVILS, STAGE_ARGUMENT, PLATFORMS_ARGUMENT, "-Pversion=$RELEASE")

            result.task(":kreateJniBuild") shouldBe null
            result.task(":kreateJniConfigure") shouldBe null
        }

        test("keeps the main POM free of the platform artifacts") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.writeMavenTargetBuild()

            fixture.build(PUBLISH_TO_DAVILS, STAGE_ARGUMENT, PLATFORMS_ARGUMENT, "-Pversion=$RELEASE")

            val pom = fixture.releasedModule("sample").resolve("sample-$RELEASE.pom")
            pom.readText() shouldNotContain "sample-linux"
        }

        test("uploads every platform artifact before the library itself") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.withEnvironment("KREATE_TEST_MAVEN_NAME", "publisher")
            fixture.withEnvironment("KREATE_TEST_MAVEN_SECRET", "test-secret")
            fixture.writeMavenTargetBuild()

            val result = fixture.build("publish", "--dry-run", STAGE_ARGUMENT, PLATFORMS_ARGUMENT, "-Pversion=$RELEASE")

            val lines = result.output.lines()
            val mainUpload = lines.indexOfFirst { line -> line.startsWith(":publishMavenPublicationToDavils") }
            val platformUploads = lines.withIndex().filter { (_, line) -> line.startsWith(":publishKreateNative") }
            platformUploads.size shouldBe ALL_PLATFORMS.size
            platformUploads.forEach { (index, _) -> mainUpload shouldBeGreaterThan index }
        }

        test("signs every platform publication when the target signs") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.writeMavenTargetBuild("signPublications = true")

            val result = fixture.build("publish", "--dry-run", STAGE_ARGUMENT, PLATFORMS_ARGUMENT, "-Pversion=$RELEASE")

            result.output shouldContain ":signMavenPublication"
            result.output shouldContain ":signKreateNativeLinuxX86_64Publication"
            result.output shouldContain ":signKreateNativeWindowsAarch64Publication"
            result.output shouldContain ":signKreateNativeMacosAarch64Publication"
        }

        test("registers every platform publication for Maven Central") {
            val fixture = newFixture()
            fixture.stageAll()
            fixture.writeBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    ${jniBlock()}

                    project {
                        name = "sample"
                        description = "Fixture"

                        publish {
                            enabled = true
                            website = "https://example.com"

                            repositories { mavenCentral { } }
                        }
                    }
                """.trimIndent()
            )

            val result = fixture.build(
                "publishToMavenCentral",
                "--dry-run",
                STAGE_ARGUMENT,
                PLATFORMS_ARGUMENT,
                "-Pversion=$RELEASE"
            )

            result.output shouldContain ":publishKreateNativeLinuxX86_64PublicationToMavenCentralRepository"
            result.output shouldContain ":publishKreateNativeMacosAarch64PublicationToMavenCentralRepository"
            result.output shouldContain ":signKreateNativeWindowsX86_64Publication"
            result.output shouldContain ":kreateJniNativeSourcesJar"
            result.output shouldContain ":kreateJniNativeJavadocJar"
        }

        test("publishes a multiplatform library together with its platform artifacts") {
            val fixture = newFixture(withJvmSource = false)
            fixture.writeKotlin(
                "jvmMain",
                "com/example/Sample.kt",
                """
                package com.example

                class Sample
                """.trimIndent()
            )
            fixture.stage("linux-x86_64")
            val releases = fixture.file("repository/releases").toURI()
            fixture.writeMultiplatformBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    ${jniBlock()}

                    project {
                        name = "sample"
                        description = "Fixture"

                        publish {
                            enabled = true
                            website = "https://example.com"

                            repositories {
                                maven {
                                    name = "Davils"
                                    releases = "$releases"
                                }
                            }
                        }
                    }
                """.trimIndent(),
                extraPlugins = listOf("""`maven-publish`""")
            )

            val result = fixture.build(
                PUBLISH_TO_DAVILS,
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.task(":$PUBLISH_TO_DAVILS")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.releasedModule("sample-jvm").isDirectory shouldBe true
            fixture.releasedModule("sample-linux-x86_64").isDirectory shouldBe true
        }
    }

    context("Checking what was staged") {
        test("refuses a library staged for another version") {
            val fixture = newFixture()
            fixture.stage("linux-x86_64", version = "0.9.0")
            fixture.writeMavenTargetBuild()

            val result = fixture.buildAndFail(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.output shouldContain "linux-x86_64 was staged for the version \"0.9.0\""
        }

        test("refuses a library that changed after it was staged") {
            val fixture = newFixture()
            val library = fixture.stage("linux-x86_64")
            library.appendText(" and something else")
            fixture.writeMavenTargetBuild()

            val result = fixture.buildAndFail(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.output shouldContain "with a SHA-256 its manifest does not give"
        }

        test("refuses a staged file the manifest does not list") {
            val fixture = newFixture()
            fixture.stage("linux-x86_64")
            fixture.write("stage/sample/linux-x86_64/libplanted.so", "not built by this release")
            fixture.writeMavenTargetBuild()

            val result = fixture.buildAndFail(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.output shouldContain "\"libplanted.so\", which its manifest does not list"
        }

        test("refuses a release whose natives were built from another commit") {
            val fixture = newFixture()
            fixture.writeMavenTargetBuild()
            git(fixture.rootDirectory, "init", "--quiet")
            git(fixture.rootDirectory, "add", ".")
            git(fixture.rootDirectory, "commit", "--quiet", "-m", "release")
            fixture.stage("linux-x86_64", revision = "0123456789abcdef0123456789abcdef01234567")

            val result = fixture.buildAndFail(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.output shouldContain "was built from the commit \"0123456789abcdef0123456789abcdef01234567\""
        }

        test("only warns about another commit for a snapshot") {
            val fixture = newFixture()
            fixture.writeMavenTargetBuild()
            git(fixture.rootDirectory, "init", "--quiet")
            git(fixture.rootDirectory, "add", ".")
            git(fixture.rootDirectory, "commit", "--quiet", "-m", "snapshot")
            fixture.stage(
                "linux-x86_64",
                version = SNAPSHOT,
                revision = "0123456789abcdef0123456789abcdef01234567"
            )

            val result = fixture.build(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$SNAPSHOT"
            )

            result.output shouldContain "was built from the commit"
        }

        test("accepts natives staged from the commit being released") {
            val fixture = newFixture()
            fixture.writeMavenTargetBuild()
            git(fixture.rootDirectory, "init", "--quiet")
            git(fixture.rootDirectory, "add", ".")
            git(fixture.rootDirectory, "commit", "--quiet", "-m", "release")
            val head = git(fixture.rootDirectory, "rev-parse", "HEAD")
            fixture.stage("linux-x86_64", revision = head)

            val result = fixture.build(
                "kreateJniVerifyPlatforms",
                STAGE_ARGUMENT,
                "-Pkreate.jni.publishPlatforms=linux-x86_64",
                "-Pversion=$RELEASE"
            )

            result.task(":kreateJniVerifyPlatforms")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldNotContain "cannot be matched to a commit"
        }
    }

    context("Staging on a runner").config(enabledOrReasonIf = requiresCmake) {
        test("stages the host library with a manifest the publishing build accepts") {
            val fixture = newFixture()
            fixture.writeMavenTargetBuild()
            fixture.writeKotlin(
                "com/example/Native.kt",
                """
                package com.example

                class Native {
                    external fun greet(): String
                }
                """.trimIndent()
            )

            fixture.build("kreateJniStage", "-Pversion=$RELEASE")

            val stageDirectory = fixture.file("build/kreate/jni-stage/sample")
            val stagedPlatforms = stageDirectory.listFiles().orEmpty()
            val staged = stagedPlatforms.single()
            val manifest = staged.resolve("native.properties").readText()
            manifest shouldContain "platform=${staged.name}"
            manifest shouldContain "artifact=sample"
            manifest shouldContain "version=$RELEASE"

            val result = fixture.build(
                "kreateJniVerifyPlatforms",
                "-Pkreate.jni.stage=build/kreate/jni-stage",
                "-Pkreate.jni.publishPlatforms=${staged.name}",
                "-Pversion=$RELEASE"
            )
            result.task(":kreateJniVerifyPlatforms")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("stages under the library's artifact while the library keeps its native name") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings(projectName = "pipe-core")
            fixture.writeMavenTargetBuild(named = false)
            fixture.writeKotlin(
                "com/example/Native.kt",
                """
                package com.example

                class Native {
                    external fun greet(): String
                }
                """.trimIndent()
            )

            fixture.build("kreateJniStage", "-Pversion=$RELEASE")

            val stageDirectory = fixture.file("build/kreate/jni-stage/pipe-core")
            val stagedPlatforms = stageDirectory.listFiles().orEmpty()
            val staged = stagedPlatforms.single()
            staged.resolve(libraryFileOf(staged.name, "pipe_core")).isFile shouldBe true
            staged.resolve("native.properties").readText() shouldContain "artifact=pipe-core"
            fixture.file("build/kreate/jni-stage/pipe_core").exists() shouldBe false
        }

        test("bundles the natives into the library JAR only, never into its sources") {
            val fixture = newFixture()
            fixture.writeBuild(
                kreateBlock = """
                    ${KreateBuildFixture.platformBlock}

                    platform {
                        jvm {
                            jni {
                                enabled = true
                                nameOverride = "sample"
                                packaging { enabled = true }
                            }
                        }
                    }

                    project {
                        name = "sample"
                        description = "Fixture"
                    }
                """.trimIndent(),
                extra = "java { withSourcesJar() }"
            )

            fixture.build("jar", "sourcesJar")

            val built = fixture.file("build/libs").listFiles()
            val libraries = built.orEmpty()
            val sources = libraries.single { file -> file.name.endsWith("-sources.jar") }
            val classes = libraries.single { file -> file.extension == "jar" && file != sources }
            jarEntries(classes).any { entry -> entry.startsWith("native/") } shouldBe true
            jarEntries(sources).none { entry -> entry.startsWith("native/") } shouldBe true
        }
    }
})
