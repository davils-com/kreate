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
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val NATIVE_EXTENSIONS: Set<String> = setOf("so", "dylib", "dll")

/**
 * End-to-end tests for the JNI pipeline.
 *
 * Every test in this class corresponds to a defect that was reproduced against the previous
 * implementation. They exist to make sure those specific failures cannot come back: each
 * one failed before the 2.0.0 rewrite and passes after it.
 */
@DisplayName("JNI pipeline")
@EnabledIfCmakeAvailable
class JniFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            platform {
                jvm {
                    jni {
                        enabled = true
                        nameOverride = "sample"
                    }
                }
            }

            project {
                name = "Sample"
                description = "JNI fixture"
            }
            """.trimIndent()
        )
        fixture.writeKotlin(
            "com/example/Native.kt",
            """
            package com.example

            class Native {
                external fun greet(): String
            }
            """.trimIndent()
        )
    }

    private fun writeNativeSource(message: String) {
        fixture.write(
            "jni/sample/src/sample.cpp",
            """
            #include "sample_jni.h"

            JNIEXPORT jstring JNICALL Java_com_example_Native_greet(JNIEnv* env, jobject receiver) {
                return env->NewStringUTF("$message");
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("scaffolds, generates headers, configures and builds in one run")
    fun buildsEndToEnd() {
        val result = fixture.build("kreateJniBuild")

        result.task(":kreateJniInitialize")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":kreateJniHeaders")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("generates a header whose signature matches the external declaration")
    fun generatesMatchingHeader() {
        fixture.build("kreateJniHeaders")

        val header = fixture.file("build/generated/jni/include/sample_jni.h")
        header.exists() shouldBe true

        // The mangled name is what the JVM looks up at runtime. Getting it wrong is the
        // single most common cause of UnsatisfiedLinkError in hand-written JNI code.
        header.readText() shouldContain "Java_com_example_Native_greet"
        header.readText() shouldContain "JNIEXPORT jstring JNICALL"
        header.readText() shouldContain "(JNIEnv *env, jobject receiver)"
    }

    @Test
    @DisplayName("keeps all build output out of the source tree")
    fun keepsSourceTreeClean() {
        writeNativeSource("hello")
        fixture.build("kreateJniBuild")

        // A build directory inside the source tree is what made the CMake cache travel
        // with the sources and break on relocation.
        fixture.nativeProjectDirectory.resolve("build").exists() shouldBe false
        fixture.nativeProjectDirectory.listFiles()!!.map { it.name }.sorted() shouldBe
            listOf("CMakeLists.txt", "src")
    }

    @Test
    @DisplayName("writes the shared library to a platform scoped directory under build/")
    fun writesLibraryToBuildDirectory() {
        writeNativeSource("hello")
        fixture.build("kreateJniBuild")

        val libraries = fixture.file("build/jni").walkTopDown()
            .filter { it.isFile && (it.name.startsWith("libsample") || it.name == "sample.dll") }
            .toList()

        libraries.size shouldBe 1
        libraries.single().parentFile.name shouldBe "lib"
    }

    @Test
    @DisplayName("rebuilds after a C++ source change instead of reporting UP-TO-DATE")
    fun rebuildsOnSourceChange() {
        writeNativeSource("first")
        fixture.build("kreateJniBuild")

        val unchanged = fixture.build("kreateJniBuild")
        unchanged.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.UP_TO_DATE

        writeNativeSource("second")
        val changed = fixture.build("kreateJniBuild")

        // Before 2.0.0 the task declared outputs but no relevant inputs, so Gradle
        // considered it up to date forever and the JVM kept loading a stale library.
        changed.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("reconfigures instead of failing when the project is relocated")
    fun survivesRelocation() {
        writeNativeSource("hello")
        fixture.build("kreateJniBuild")

        // Copy everything a version control checkout would carry — sources and any stale
        // native build output — but not Gradle's own state, which is path bound by design
        // and would never travel to a fresh CI agent either.
        val relocated = projectDir.parentFile.resolve("${projectDir.name}-relocated")
        projectDir.copyRecursively(relocated, overwrite = true)
        relocated.resolve(".gradle").deleteRecursively()

        // CMakeCache.txt records the absolute paths it was generated for. While the CMake
        // build directory lived inside the source tree it travelled along with it, and this
        // failed irrecoverably with "The current CMakeCache.txt directory ... is different
        // ..." — which `gradle clean` could not repair, because it never reached in there.
        val result = KreateBuildFixture(relocated).build("kreateJniBuild")

        result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        relocated.deleteRecursively()
    }

    @Test
    @DisplayName("reports the CMake output when the native build fails")
    fun reportsCompilerDiagnostics() {
        fixture.write(
            "jni/sample/src/sample.cpp",
            """
            #include "sample_jni.h"

            this is not valid c++
            """.trimIndent()
        )

        val result = fixture.buildAndFail("kreateJniBuild")

        // A bare exit code gives the user nothing to act on; the compiler diagnostic does.
        result.output shouldContain "CMake build failed"
        result.output shouldContain "Output:"
    }

    @Test
    @DisplayName("skips reconfiguring when only C++ sources changed")
    fun reusesConfiguration() {
        writeNativeSource("first")
        fixture.build("kreateJniBuild")

        writeNativeSource("second")
        val result = fixture.build("kreateJniBuild")

        result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.UP_TO_DATE
        result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("does not run the native build as part of Kotlin compilation")
    fun nativeBuildIsNotOnTheCompilePath() {
        // Nothing on the JVM side needs the shared library until code is executed, and
        // hooking it into compilation is what made header generation impossible.
        val result = fixture.build("compileKotlin")

        result.task(":kreateJniBuild") shouldBe null
    }

    @Test
    @DisplayName("packages the library and generates a loader when packaging is enabled")
    fun packagesNatives() {
        writeNativeSource("hello")
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            platform {
                jvm {
                    jni {
                        enabled = true
                        nameOverride = "sample"
                        packaging {
                            enabled = true
                            generateLoader = true
                        }
                    }
                }
            }

            project {
                name = "Sample"
                description = "JNI fixture"
            }
            """.trimIndent()
        )

        fixture.build("jar")

        val jar = fixture.file("build/libs").listFiles()!!.single { it.extension == "jar" }
        val entries = java.util.zip.ZipFile(jar).use { zip -> zip.entries().toList().map { it.name } }

        entries.any { it.startsWith("native/") && it.contains("sample") } shouldBe true
        entries.any { it.endsWith("KreateNativeLoader.class") } shouldBe true
        entries.any { it == "native/digests.properties" } shouldBe true
    }

    @Test
    @DisplayName("the digest manifest carries the SHA-256 of the library that was packaged")
    fun writesDigestManifest() {
        writeNativeSource("hello")
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            platform {
                jvm {
                    jni {
                        enabled = true
                        nameOverride = "sample"
                        packaging {
                            enabled = true
                        }
                    }
                }
            }

            project {
                name = "Sample"
                description = "JNI fixture"
            }
            """.trimIndent()
        )

        fixture.build("jar")

        val jar = fixture.file("build/libs").listFiles()!!.single { it.extension == "jar" }
        val manifest = java.util.zip.ZipFile(jar).use { zip ->
            val entry = zip.getEntry("native/digests.properties")
            zip.getInputStream(entry).readBytes().decodeToString()
        }
        val library = fixture.file("build/jni").walkTopDown()
            .single { file -> file.isFile && file.name.contains("sample") && file.extension in NATIVE_EXTENSIONS }
        val expected = java.security.MessageDigest.getInstance("SHA-256")
            .digest(library.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }

        manifest shouldContain "=$expected"
    }

    @Test
    @DisplayName("the generated loader is off unless a build asks for it")
    fun loaderIsOptIn() {
        writeNativeSource("hello")
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            platform {
                jvm {
                    jni {
                        enabled = true
                        nameOverride = "sample"
                        packaging {
                            enabled = true
                        }
                    }
                }
            }

            project {
                name = "Sample"
                description = "JNI fixture"
            }
            """.trimIndent()
        )

        fixture.build("jar")

        val jar = fixture.file("build/libs").listFiles()!!.single { it.extension == "jar" }
        val entries = java.util.zip.ZipFile(jar).use { zip -> zip.entries().toList().map { it.name } }

        entries.none { it.endsWith("KreateNativeLoader.class") } shouldBe true
    }

    @Test
    @DisplayName("compiles the native code against the toolchain JDK")
    fun usesToolchainJdk() {
        writeNativeSource("hello")
        fixture.build("kreateJniConfigure")

        val cache = fixture.file("build/jni").walkTopDown().single { it.name == "CMakeCache.txt" }

        // Left to itself, find_package(JNI) picks whatever JDK the machine defaults to and
        // silently compiles against different headers than the Kotlin code targets.
        val javaHomeEntry = cache.readLines().single { it.startsWith("JAVA_HOME:") }
        val javaHome = File(javaHomeEntry.substringAfter('='))

        javaHome.isDirectory shouldBe true
        javaHome.resolve("include/jni.h").exists() shouldBe true
    }
}
