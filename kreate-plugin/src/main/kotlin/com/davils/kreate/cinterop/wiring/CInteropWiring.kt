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
import com.davils.kreate.cinterop.CInteropExtension
import com.davils.kreate.cinterop.CInteropTaskNames
import com.davils.kreate.cinterop.NativeLanguage
import com.davils.kreate.cinterop.rust.resolveRustTargets
import com.davils.kreate.cinterop.task.AddRustDependencies
import com.davils.kreate.cinterop.task.BUILD_RUST_FILE_NAME
import com.davils.kreate.cinterop.task.CARGO_TOML_FILE_NAME
import com.davils.kreate.cinterop.task.CompileNative
import com.davils.kreate.cinterop.task.CompileRust
import com.davils.kreate.cinterop.task.ConfigureCargo
import com.davils.kreate.cinterop.task.GenerateDefinitionFiles
import com.davils.kreate.cinterop.task.GenerateRustBuildScript
import com.davils.kreate.cinterop.task.InitializeNativeProject
import com.davils.kreate.cinterop.task.InitializeRustProject
import com.davils.kreate.gradle.resolveFeatureProjectName
import com.davils.kreate.gradle.resolveFeatureRootDirectory
import com.davils.kreate.task.CMAKE_LISTS_FILE_NAME
import com.davils.kreate.task.executeTaskBeforeCompile
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

private const val CINTEROP_DIRECTORY_NAME: String = "cinterop"

private val WINDOWS_TARGET_MARKERS: List<String> = listOf("x86_64-pc-windows", "mingw")

private const val MACOS_ARM64_TARGET_MARKER: String = "aarch64-apple-darwin"

private const val LINUX_X64_TARGET_MARKER: String = "x86_64-unknown-linux"

private const val LINUX_ARM64_TARGET_MARKER: String = "aarch64-unknown-linux"

internal fun Project.initializeCInterop(extension: KreateExtension) {
    val cInteropConfig = extension.platform.multiplatform.cInterop
    if (!cInteropConfig.enabled.get()) return

    val projectName = resolveFeatureProjectName(extension, cInteropConfig.nameOverride)
    val projectRootDir = resolveFeatureRootDirectory(cInteropConfig.projectDirectory, CINTEROP_DIRECTORY_NAME)

    addCInteropTasks(cInteropConfig, projectName, projectRootDir)
    applyNativeTargets(cInteropConfig, projectName, projectRootDir)
}

private fun Project.addCInteropTasks(cInteropConfig: CInteropExtension, projectName: String, projectRootDir: File) {
    val generateDefinitionFiles = when (cInteropConfig.language.get()) {
        NativeLanguage.RUST -> addRustCInteropTasks(cInteropConfig, projectName, projectRootDir)
        NativeLanguage.C, NativeLanguage.CPP -> addNativeCInteropTasks(cInteropConfig, projectName, projectRootDir)
    }

    tasks.withType<CInteropProcess>().configureEach {
        dependsOn(generateDefinitionFiles)
    }
    executeTaskBeforeCompile(generateDefinitionFiles)
}

private fun Project.addRustCInteropTasks(
    cInteropConfig: CInteropExtension,
    projectName: String,
    projectRootDir: File
): TaskProvider<GenerateDefinitionFiles> {
    val rustProject = projectRootDir.resolve(projectName)
    val initializeRustProject = tasks.register<InitializeRustProject>(CInteropTaskNames.INITIALIZE) {
        this.workDir.set(projectRootDir)
        this.projectName.set(projectName)
    }

    val addRustDependencies = tasks.register<AddRustDependencies>(CInteropTaskNames.DEPENDENCIES) {
        workDir.set(rustProject)
        dependsOn(initializeRustProject)
    }

    val configureCargo = tasks.register<ConfigureCargo>(CInteropTaskNames.CONFIGURE) {
        workDir.set(rustProject)
        dependsOn(addRustDependencies)
    }

    val generateRustBuildScript = tasks.register<GenerateRustBuildScript>(CInteropTaskNames.SCRIPT) {
        workDir.set(rustProject)
        this.projectName.set(projectName)
        dependsOn(configureCargo)
    }

    val compileRust = tasks.register<CompileRust>(CInteropTaskNames.COMPILE) {
        workDir.set(rustProject)
        nativeSources.from(
            fileTree(rustProject) {
                include(CARGO_TOML_FILE_NAME, BUILD_RUST_FILE_NAME, "src/**")
            }
        )
        if (cInteropConfig.rustTargets.isPresent) {
            rustTargets.set(cInteropConfig.rustTargets)
        }
        dependsOn(generateRustBuildScript)
    }

    return tasks.register<GenerateDefinitionFiles>(CInteropTaskNames.DEFINITIONS) {
        configureDefinitionFiles(cInteropConfig, rustProject, projectRootDir, projectName)
        if (cInteropConfig.rustTargets.isPresent) {
            rustTargets.set(cInteropConfig.rustTargets)
        }
        dependsOn(compileRust)
    }
}

private fun Project.addNativeCInteropTasks(
    cInteropConfig: CInteropExtension,
    projectName: String,
    projectRootDir: File
): TaskProvider<GenerateDefinitionFiles> {
    val nativeProject = projectRootDir.resolve(projectName)
    val initializeNativeProject = tasks.register<InitializeNativeProject>(CInteropTaskNames.INITIALIZE) {
        this.workDir.set(projectRootDir)
        this.projectName.set(projectName)
        this.language.set(cInteropConfig.language)
    }

    val compileNative = tasks.register<CompileNative>(CInteropTaskNames.COMPILE) {
        this.workDir.set(nativeProject)
        nativeSources.from(
            fileTree(nativeProject) {
                include(CMAKE_LISTS_FILE_NAME, "src/**", "include/**")
            }
        )
        dependsOn(initializeNativeProject)
    }

    return tasks.register<GenerateDefinitionFiles>(CInteropTaskNames.DEFINITIONS) {
        configureDefinitionFiles(cInteropConfig, nativeProject, projectRootDir, projectName)
        dependsOn(compileNative)
    }
}

private fun GenerateDefinitionFiles.configureDefinitionFiles(
    cInteropConfig: CInteropExtension,
    nativeProject: File,
    projectRootDir: File,
    projectName: String
) {
    workDir.set(nativeProject)
    rootDir.set(projectRootDir)
    this.projectName.set(projectName)
    defFileName.set(cInteropConfig.defFiles.fileName)
    defDirName.set(cInteropConfig.defFiles.dirName)
    language.set(cInteropConfig.language)
}

private fun Project.applyNativeTargets(cInteropConfig: CInteropExtension, projectName: String, projectRootDir: File) {
    val rustTargets = resolveRustTargets(cInteropConfig.rustTargets.getOrElse(emptyList()))
    val gradleProject = this
    val configureInterop: KotlinNativeTarget.() -> Unit = {
        configureCInterop(gradleProject, cInteropConfig, projectName, projectRootDir)
    }

    configure<KotlinMultiplatformExtension> {
        rustTargets.forEach { rustTarget -> registerNativeTarget(rustTarget, cInteropConfig, configureInterop) }
    }
}

private fun KotlinMultiplatformExtension.registerNativeTarget(
    rustTarget: String,
    cInteropConfig: CInteropExtension,
    configureInterop: KotlinNativeTarget.() -> Unit
) {
    val isWindowsTarget = WINDOWS_TARGET_MARKERS.any { marker -> marker in rustTarget }
    if (isWindowsTarget) {
        mingwX64 { configureNativeTarget(configureInterop, cInteropConfig.mingwConfiguration) }
        return
    }
    if (MACOS_ARM64_TARGET_MARKER in rustTarget) {
        macosArm64 { configureNativeTarget(configureInterop, cInteropConfig.macosConfiguration) }
        return
    }
    if (LINUX_X64_TARGET_MARKER in rustTarget) {
        linuxX64 { configureNativeTarget(configureInterop, cInteropConfig.linuxConfiguration) }
        return
    }
    if (LINUX_ARM64_TARGET_MARKER in rustTarget) {
        linuxArm64 { configureNativeTarget(configureInterop, cInteropConfig.linuxConfiguration) }
        return
    }

    throw GradleException("Unsupported Rust target for Kotlin/Native mapping: $rustTarget")
}

private fun KotlinNativeTarget.configureNativeTarget(
    configureInterop: KotlinNativeTarget.() -> Unit,
    userConfiguration: KotlinNativeTarget.() -> Unit
) {
    configureInterop()
    userConfiguration()
}
