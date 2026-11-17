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

package com.davils.kreate.platform.wiring

import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val JVM_PLUGIN: String = "org.jetbrains.kotlin.jvm"

private fun Project.kotlinCompile(): KotlinJvmCompile = tasks.getByName("compileKotlin") as KotlinJvmCompile

private fun Project.javaCompile(): JavaCompile = tasks.getByName("compileJava") as JavaCompile

private fun Project.jar(): Jar = tasks.getByName("jar") as Jar

class CompilerOptionsTest : FunSpec({
    val workspace = tempdir()

    context("the Kotlin compiler") {
        test("keeps the defaults of the Kotlin Gradle plugin when nothing is configured") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { }
            val options = project.kotlinCompile().compilerOptions

            options.progressiveMode.get() shouldBe false
            options.extraWarnings.get() shouldBe false
            options.languageVersion.orNull.shouldBeNull()
        }

        test("takes every configured option") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                kreate.platform.compiler {
                    progressiveMode.set(true)
                    extraWarnings.set(true)
                    languageVersion.set(KotlinVersion.KOTLIN_2_3)
                    apiVersion.set(KotlinVersion.KOTLIN_2_3)
                    jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
                }
            }
            val options = project.kotlinCompile().compilerOptions

            options.progressiveMode.get() shouldBe true
            options.extraWarnings.get() shouldBe true
            options.languageVersion.get() shouldBe KotlinVersion.KOTLIN_2_3
            options.apiVersion.get() shouldBe KotlinVersion.KOTLIN_2_3
            options.jvmDefault.get() shouldBe JvmDefaultMode.NO_COMPATIBILITY
        }
    }

    context("javac") {
        test("keeps its defaults unless warnings are errors") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { }
            val options = project.javaCompile().options

            options.release.orNull.shouldBeNull()
            options.compilerArgs shouldNotContain "-Werror"
        }

        test("compiles for the platform release and fails on every lint warning") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                kreate.platform.javaVersion.set(JavaVersion.VERSION_21)
                kreate.platform.jvm { javaWarningsAsErrors.set(true) }
            }
            val options = project.javaCompile().options

            options.release.get() shouldBe 21
            options.compilerArgs shouldContainAll listOf("-Xlint:all", "-Werror")
        }
    }

    context("the Java module") {
        test("names the jar as an automatic module when there is no descriptor") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                kreate.platform.jvm { moduleName.set("com.example.library") }
            }

            project.jar().manifest.attributes["Automatic-Module-Name"] shouldBe "com.example.library"
            val providers = project.javaCompile().options.compilerArgumentProviders
            providers.filterIsInstance<ModulePatch>() shouldBe emptyList()
        }

        test("patches the Kotlin classes into the module of the descriptor") {
            val directory = workspace.freshDirectory()
            val descriptor = directory.resolve("src/main/java/module-info.java")
            descriptor.parentFile.mkdirs()
            descriptor.writeText("module com.example.library {}\n")
            val project = evaluatedKreateProject(directory, JVM_PLUGIN) { kreate ->
                kreate.platform.jvm { moduleName.set("com.example.library") }
            }
            val providers = project.javaCompile().options.compilerArgumentProviders
            val arguments = providers.flatMap { provider -> provider.asArguments() }
            val kotlinClasses = project.kotlinCompile().destinationDirectory.get().asFile

            arguments shouldContainExactly listOf("--patch-module", "com.example.library=${kotlinClasses.absolutePath}")
            project.jar().manifest.attributes["Automatic-Module-Name"].shouldBeNull()
        }

        test("refuses a name that is no Java module name") {
            val failure = shouldThrow<Exception> {
                evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                    kreate.platform.jvm { moduleName.set("com.example.1library") }
                }
            }

            val rootCause = generateSequence<Throwable>(failure) { cause -> cause.cause }.last()

            rootCause.message.orEmpty() shouldContain "no Java module name"
        }
    }
})
