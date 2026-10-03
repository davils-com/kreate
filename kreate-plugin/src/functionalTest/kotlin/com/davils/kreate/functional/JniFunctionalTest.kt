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
import java.security.MessageDigest
import java.util.zip.ZipFile

private val NATIVE_EXTENSIONS: Set<String> = setOf("so", "dylib", "dll")

class JniFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
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
        return fixture
    }

    fun KreateBuildFixture.writeNativeSource(message: String) {
        write(
            "jni/sample/src/sample.cpp",
            """
            #include "sample_jni.h"

            JNIEXPORT jstring JNICALL Java_com_example_Native_greet(JNIEnv* env, jobject receiver) {
                return env->NewStringUTF("$message");
            }
            """.trimIndent()
        )
    }

    fun builtJar(fixture: KreateBuildFixture): File {
        val libraryDirectory = fixture.file("build/libs")
        val libraries = libraryDirectory.listFiles().orEmpty()
        return libraries.single { it.extension == "jar" }
    }

    fun jarEntries(jar: File): List<String> = ZipFile(jar).use { zip ->
        val entries = zip.entries().toList()
        entries.map { it.name }
    }

    context("JNI pipeline").config(enabledOrReasonIf = requiresCmake) {
        test("scaffolds, generates headers, configures and builds in one run") {
            val fixture = newFixture()

            val result = fixture.build("kreateJniBuild")

            result.task(":kreateJniInitialize")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":kreateJniHeaders")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("generates a header whose signature matches the external declaration") {
            val fixture = newFixture()
            fixture.build("kreateJniHeaders")

            val header = fixture.file("build/generated/jni/include/sample_jni.h")
            header.exists() shouldBe true

            val mangledNameTheJvmLooksUp = "Java_com_example_Native_greet"
            header.readText() shouldContain mangledNameTheJvmLooksUp
            header.readText() shouldContain "JNIEXPORT jstring JNICALL"
            header.readText() shouldContain "(JNIEnv *env, jobject receiver)"
        }

        test("keeps all build output out of the source tree") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
            fixture.build("kreateJniBuild")

            fixture.nativeProjectDirectory.resolve("build").exists() shouldBe false
            val nativeProjectEntries = fixture.nativeProjectDirectory.list().orEmpty()
            nativeProjectEntries.sorted() shouldBe listOf("CMakeLists.txt", "src")
        }

        test("writes the shared library to a platform scoped directory under build/") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
            fixture.build("kreateJniBuild")

            val jniOutput = fixture.file("build/jni").walkTopDown()
            val libraries = jniOutput
                .filter { it.isFile && (it.name.startsWith("libsample") || it.name == "sample.dll") }
                .toList()

            libraries.size shouldBe 1
            libraries.single().parentFile.name shouldBe "lib"
        }

        test("rebuilds after a C++ source change instead of reporting UP-TO-DATE") {
            val fixture = newFixture()
            fixture.writeNativeSource("first")
            fixture.build("kreateJniBuild")

            val unchanged = fixture.build("kreateJniBuild")
            unchanged.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.UP_TO_DATE

            fixture.writeNativeSource("second")
            val changed = fixture.build("kreateJniBuild")

            changed.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("reconfigures instead of failing when the project is relocated") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
            fixture.build("kreateJniBuild")

            val projectDir = fixture.rootDirectory
            val relocated = projectDir.parentFile.resolve("${projectDir.name}-relocated")
            projectDir.copyRecursively(relocated, overwrite = true)
            val pathBoundGradleState = relocated.resolve(".gradle")
            pathBoundGradleState.deleteRecursively()

            val result = KreateBuildFixture(relocated).build("kreateJniBuild")

            result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.SUCCESS
            result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
            relocated.deleteRecursively()
        }

        test("reports the CMake output when the native build fails") {
            val fixture = newFixture()
            fixture.write(
                "jni/sample/src/sample.cpp",
                """
                #include "sample_jni.h"

                this is not valid c++
                """.trimIndent()
            )

            val result = fixture.buildAndFail("kreateJniBuild")

            result.output shouldContain "CMake build failed"
            result.output shouldContain "Output:"
        }

        test("skips reconfiguring when only C++ sources changed") {
            val fixture = newFixture()
            fixture.writeNativeSource("first")
            fixture.build("kreateJniBuild")

            fixture.writeNativeSource("second")
            val result = fixture.build("kreateJniBuild")

            result.task(":kreateJniConfigure")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            result.task(":kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("does not run the native build as part of Kotlin compilation") {
            val fixture = newFixture()

            val result = fixture.build("compileKotlin")

            result.task(":kreateJniBuild") shouldBe null
        }

        test("packages the library and generates a loader when packaging is enabled") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
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

            val entries = jarEntries(builtJar(fixture))

            entries.any { it.startsWith("native/") && it.contains("sample") } shouldBe true
            entries.any { it.endsWith("KreateNativeLoader.class") } shouldBe true
            entries.any { it == "native/digests.properties" } shouldBe true
        }

        test("the digest manifest carries the SHA-256 of the library that was packaged") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
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

            val jar = builtJar(fixture)
            val manifest = ZipFile(jar).use { zip ->
                val entry = zip.getEntry("native/digests.properties")
                val bytes = zip.getInputStream(entry).readBytes()
                bytes.decodeToString()
            }
            val jniOutput = fixture.file("build/jni").walkTopDown()
            val library = jniOutput.single { file ->
                file.isFile && file.name.contains("sample") && file.extension in NATIVE_EXTENSIONS
            }
            val sha256 = MessageDigest.getInstance("SHA-256")
            val expected = sha256.digest(library.readBytes()).joinToString("") { byte -> "%02x".format(byte) }

            manifest shouldContain "=$expected"
        }

        test("the generated loader is off unless a build asks for it") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
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

            val entries = jarEntries(builtJar(fixture))

            entries.none { it.endsWith("KreateNativeLoader.class") } shouldBe true
        }

        test("compiles the native code against the toolchain JDK") {
            val fixture = newFixture()
            fixture.writeNativeSource("hello")
            fixture.build("kreateJniConfigure")

            val jniOutput = fixture.file("build/jni").walkTopDown()
            val cache = jniOutput.single { it.name == "CMakeCache.txt" }

            val javaHomeEntry = cache.readLines().single { it.startsWith("JAVA_HOME:") }
            val javaHome = File(javaHomeEntry.substringAfter('='))

            javaHome.isDirectory shouldBe true
            javaHome.resolve("include/jni.h").exists() shouldBe true
        }
    }
})
