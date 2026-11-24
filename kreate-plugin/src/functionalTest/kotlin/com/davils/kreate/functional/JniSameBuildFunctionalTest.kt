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
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.util.zip.ZipFile

class JniSameBuildFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun kreateBuild(kreateBlock: String, extra: String = ""): String =
        """
        plugins {
            id("org.jetbrains.kotlin.jvm")
            id("com.davils.kreate")
        }

        group = "com.example"

        kreate {
            ${KreateBuildFixture.platformBlock}

            $kreateBlock
        }

        $extra
        """.trimIndent()

    fun javaExec(mainClass: String): String =
        """
        tasks.register<JavaExec>("runMain") {
            classpath = sourceSets.main.get().runtimeClasspath
            mainClass.set("$mainClass")
        }
        """.trimIndent()

    fun KreateBuildFixture.writeCore() {
        val kreateBlock = """
            platform {
                jvm {
                    jni {
                        enabled = true
                        nameOverride = "sample"
                        packaging {
                            enabled = true
                            generateLoader = true
                            publishing {
                                enabled = true
                            }
                        }
                    }
                }
            }

            project {
                name = "Sample"
                description = "The JNI project"
            }
        """.trimIndent()
        write("core/build.gradle.kts", kreateBuild(kreateBlock))
        write(
            "core/src/main/kotlin/com/example/Native.kt",
            """
            package com.example

            import com.example.sample.jni.KreateNativeLoader

            class Native {
                external fun greet(): String

                companion object {
                    init {
                        KreateNativeLoader.load("sample")
                    }
                }
            }
            """.trimIndent()
        )
        write(
            "core/jni/sample/src/sample.cpp",
            """
            #include "sample_jni.h"

            JNIEXPORT jstring JNICALL Java_com_example_Native_greet(JNIEnv* env, jobject receiver) {
                return env->NewStringUTF("hello from the host library");
            }
            """.trimIndent()
        )
    }

    fun KreateBuildFixture.writeApp() {
        val kreateBlock = """
            project {
                name = "App"
                description = "Depends on the JNI project"
            }
        """.trimIndent()
        val dependencies = """
            dependencies {
                implementation(project(":core"))
            }
        """.trimIndent()
        val extra = listOf(dependencies, javaExec("com.example.app.MainKt")).joinToString(System.lineSeparator())
        write("app/build.gradle.kts", kreateBuild(kreateBlock, extra))
        write(
            "app/src/main/kotlin/com/example/app/Main.kt",
            """
            package com.example.app

            import com.example.Native

            fun main() {
                println("native: " + Native().greet())
            }
            """.trimIndent()
        )
    }

    fun KreateBuildFixture.writePlain() {
        val kreateBlock = """
            project {
                name = "Plain"
                description = "Depends on no JNI project"
            }
        """.trimIndent()
        write("plain/build.gradle.kts", kreateBuild(kreateBlock, javaExec("com.example.plain.MainKt")))
        write(
            "plain/src/main/kotlin/com/example/plain/Main.kt",
            """
            package com.example.plain

            fun main() {
                println("useLibraryPath: " + System.getProperty("kreate.jni.useLibraryPath"))
            }
            """.trimIndent()
        )
    }

    fun newBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        val settings = fixture.file("settings.gradle.kts").readText()
        val included = """include("core", "app", "plain")"""
        fixture.write("settings.gradle.kts", listOf(settings, included).joinToString(System.lineSeparator()))
        fixture.writeCore()
        fixture.writeApp()
        fixture.writePlain()
        return fixture
    }

    fun nativeEntries(jar: File): List<String> = ZipFile(jar).use { zip ->
        val entries = zip.entries().toList()
        val names = entries.map { entry -> entry.name }
        names.filter { name -> name.startsWith("native/") && !name.endsWith("/") }
    }

    context("JNI projects of the same build").config(enabledOrReasonIf = requiresCmake) {
        test("a project loads the host library of a JNI project it depends on") {
            val fixture = newBuild()

            val result = fixture.build(":app:runMain")

            result.task(":core:kreateJniBuild")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "native: hello from the host library"
        }

        test("the JAR of a JNI project that publishes per platform still carries no library") {
            val fixture = newBuild()

            fixture.build(":app:runMain", ":core:jar")

            val libraryDirectory = fixture.file("core/build/libs")
            val libraries = libraryDirectory.listFiles().orEmpty()
            val jar = libraries.single { file -> file.extension == "jar" }
            nativeEntries(jar) shouldBe emptyList()
        }

        test("a project that depends on no JNI project keeps java.library.path as it is") {
            val fixture = newBuild()

            val result = fixture.build(":plain:runMain")

            result.output shouldContain "useLibraryPath: null"
            result.output shouldNotContain ":core:kreateJniBuild"
        }

        test("a second run reuses the configuration cache") {
            val fixture = newBuild()
            fixture.build(":app:runMain")

            val result = fixture.build(":app:runMain")

            result.output shouldContain "Reusing configuration cache"
            result.output shouldContain "native: hello from the host library"
        }
    }
})
