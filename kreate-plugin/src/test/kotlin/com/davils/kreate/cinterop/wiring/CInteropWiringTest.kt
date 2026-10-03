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

package com.davils.kreate.cinterop.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreatePlugin
import com.davils.kreate.cinterop.CInteropExtension
import com.davils.kreate.cinterop.CInteropTaskNames
import com.davils.kreate.cinterop.NativeLanguage
import com.davils.kreate.cinterop.task.CompileNative
import com.davils.kreate.cinterop.task.CompileRust
import com.davils.kreate.cinterop.task.GenerateDefinitionFiles
import com.davils.kreate.cinterop.task.InitializeNativeProject
import com.davils.kreate.cinterop.task.InitializeRustProject
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

private const val INTEROP_NAME = "native_demo"
private const val LINUX_X64 = "x86_64-unknown-linux-gnu"
private const val LINUX_ARM64 = "aarch64-unknown-linux-gnu"
private const val MACOS_ARM64 = "aarch64-apple-darwin"
private const val WINDOWS_X64 = "x86_64-pc-windows-gnu"

class CInteropWiringTest : FunSpec({
    val workspace = tempdir()

    fun multiplatformProject(): Project {
        val builder = ProjectBuilder.builder()
        builder.withName("native-demo")
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        project.group = "com.acme"
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(KreatePlugin::class.java)
        return project
    }

    fun cInteropOf(project: Project): CInteropExtension {
        val kreate = project.extensions.getByType(KreateExtension::class.java)
        return kreate.platform.multiplatform.cInterop
    }

    fun evaluated(configure: (CInteropExtension) -> Unit): Project {
        val project = multiplatformProject()
        val cInterop = cInteropOf(project)
        cInterop.enabled.set(true)
        configure(cInterop)
        (project as ProjectInternal).evaluate()
        return project
    }

    fun nativeTargetsOf(project: Project): List<KotlinNativeTarget> {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val targets = kotlin.targets.withType(KotlinNativeTarget::class.java)
        return targets.toList()
    }

    fun nativeTargetNamesOf(project: Project): List<String> =
        nativeTargetsOf(project).map { target -> target.name }

    fun messagesOf(failure: Throwable): String {
        val chain = generateSequence(failure) { cause -> cause.cause }
        return chain.joinToString(" | ") { cause -> cause.message.orEmpty() }
    }

    context("C interoperation wiring") {
        test("registers nothing while the feature is disabled") {
            val project = multiplatformProject()
            (project as ProjectInternal).evaluate()

            project.tasks.findByName(CInteropTaskNames.INITIALIZE) shouldBe null
            project.tasks.findByName(CInteropTaskNames.DEFINITIONS) shouldBe null
        }

        test("registers the Cargo pipeline for a Rust library") {
            val project = evaluated { cInterop -> cInterop.rustTargets.set(listOf(LINUX_X64)) }
            val tasks = project.tasks

            tasks.getByName(CInteropTaskNames.INITIALIZE).shouldBeInstanceOf<InitializeRustProject>()
            tasks.getByName(CInteropTaskNames.COMPILE).shouldBeInstanceOf<CompileRust>()
            tasks.findByName(CInteropTaskNames.DEPENDENCIES) shouldBe tasks.getByName(CInteropTaskNames.DEPENDENCIES)
            tasks.findByName(CInteropTaskNames.CONFIGURE) shouldBe tasks.getByName(CInteropTaskNames.CONFIGURE)
            tasks.findByName(CInteropTaskNames.SCRIPT) shouldBe tasks.getByName(CInteropTaskNames.SCRIPT)
        }

        test("hands the declared Rust targets to compilation and definition") {
            val project = evaluated { cInterop -> cInterop.rustTargets.set(listOf(LINUX_X64)) }
            val tasks = project.tasks
            val compile = tasks.getByName(CInteropTaskNames.COMPILE) as CompileRust
            val definitions = tasks.getByName(CInteropTaskNames.DEFINITIONS) as GenerateDefinitionFiles

            compile.rustTargets.get() shouldBe listOf(LINUX_X64)
            definitions.rustTargets.get() shouldBe listOf(LINUX_X64)
            definitions.language.get() shouldBe NativeLanguage.RUST
        }

        test("names the interop after the project, lowercased and with underscores") {
            val project = evaluated { cInterop -> cInterop.rustTargets.set(listOf(LINUX_X64)) }
            val definitions = project.tasks.getByName(CInteropTaskNames.DEFINITIONS) as GenerateDefinitionFiles
            val root = project.projectDir.resolve("cinterop")

            definitions.projectName.get() shouldBe INTEROP_NAME
            definitions.rootDir.get().asFile shouldBe root
            definitions.workDir.get().asFile shouldBe root.resolve(INTEROP_NAME)
            definitions.defFileName.get() shouldBe "cinterop.def"
            definitions.defDirName.get() shouldBe "defs"
        }

        test("prefers the configured name and project directory") {
            val project = evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64))
                cInterop.nameOverride.set("Bridge-Lib")
                cInterop.projectDirectory.set(workspace.freshDirectory("native"))
            }
            val definitions = project.tasks.getByName(CInteropTaskNames.DEFINITIONS) as GenerateDefinitionFiles
            val root = cInteropOf(project).projectDirectory.get().asFile

            definitions.projectName.get() shouldBe "bridge_lib"
            definitions.rootDir.get().asFile shouldBe root
        }

        test("registers the CMake pipeline for a C library") {
            val project = evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64))
                cInterop.language.set(NativeLanguage.C)
            }
            val tasks = project.tasks
            val initialize = tasks.getByName(CInteropTaskNames.INITIALIZE) as InitializeNativeProject

            initialize.language.get() shouldBe NativeLanguage.C
            initialize.projectName.get() shouldBe INTEROP_NAME
            tasks.getByName(CInteropTaskNames.COMPILE).shouldBeInstanceOf<CompileNative>()
            tasks.findByName(CInteropTaskNames.DEPENDENCIES) shouldBe null
            tasks.findByName(CInteropTaskNames.SCRIPT) shouldBe null
        }

        test("registers the CMake pipeline for a C++ library") {
            val project = evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64))
                cInterop.language.set(NativeLanguage.CPP)
            }
            val definitions = project.tasks.getByName(CInteropTaskNames.DEFINITIONS) as GenerateDefinitionFiles

            definitions.language.get() shouldBe NativeLanguage.CPP
            definitions.rustTargets.get() shouldBe emptyList()
        }

        test("maps every supported Rust target onto a Kotlin/Native target") {
            val project = evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64, LINUX_ARM64, MACOS_ARM64, WINDOWS_X64))
            }

            nativeTargetNamesOf(project) shouldContainAll listOf("linuxX64", "linuxArm64", "macosArm64", "mingwX64")
        }

        test("builds for the host alone when no Rust target is declared") {
            val project = evaluated { }

            nativeTargetsOf(project) shouldHaveSize 1
        }

        test("runs the platform block the build declared for the target") {
            val configured = mutableListOf<String>()
            evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64, MACOS_ARM64, WINDOWS_X64))
                cInterop.linux { configured.add(name) }
                cInterop.macos { configured.add(name) }
                cInterop.mingw { configured.add(name) }
            }

            configured shouldContainAll listOf("linuxX64", "macosArm64", "mingwX64")
        }

        test("declares the interop on every compilation with the conventional package") {
            val project = evaluated { cInterop -> cInterop.rustTargets.set(listOf(LINUX_X64)) }
            val target = nativeTargetsOf(project).single()
            val main = target.compilations.getByName("main")
            val interop = main.cinterops.getByName(INTEROP_NAME)
            val expectedDefinition = project.projectDir.resolve("cinterop/$INTEROP_NAME/defs/cinterop.def")

            interop.packageName shouldBe "com.acme.$INTEROP_NAME.cinterop"
            interop.definitionFile.get().asFile shouldBe expectedDefinition
        }

        test("prefers the configured interop package") {
            val project = evaluated { cInterop ->
                cInterop.rustTargets.set(listOf(LINUX_X64))
                cInterop.packageNameOverride.set("com.acme.bridge")
                cInterop.defFile { fileName.set("bridge.def") }
            }
            val target = nativeTargetsOf(project).single()
            val main = target.compilations.getByName("main")
            val interop = main.cinterops.getByName(INTEROP_NAME)

            interop.packageName shouldBe "com.acme.bridge"
            interop.definitionFile.get().asFile.name shouldBe "bridge.def"
        }

        test("generates the definition files before any interop process") {
            val project = evaluated { cInterop -> cInterop.rustTargets.set(listOf(LINUX_X64)) }
            val processes = project.tasks.withType(CInteropProcess::class.java)
            val dependencies = processes.first().dependsOn
            val providers = dependencies.filterIsInstance<TaskProvider<*>>()

            providers.map { provider -> provider.name } shouldContain CInteropTaskNames.DEFINITIONS
        }

        test("rejects a Rust target Kotlin/Native has no counterpart for") {
            val failure = shouldThrowAny {
                evaluated { cInterop -> cInterop.rustTargets.set(listOf("riscv64gc-unknown-linux-gnu")) }
            }

            messagesOf(failure) shouldContain "Unsupported Rust target for Kotlin/Native mapping"
        }
    }
})
