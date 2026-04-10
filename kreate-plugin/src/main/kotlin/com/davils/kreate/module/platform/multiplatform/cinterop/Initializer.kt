package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import com.davils.kreate.jobs.executeTaskBeforeCompile
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.AddRustDependencies
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.CompileRust
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.ConfigureCargo
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.GenerateDefinitionFiles
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.GenerateRustBuildScript
import com.davils.kreate.module.platform.multiplatform.cinterop.tasks.InitializeRustProject
import com.davils.kreate.system.Architecture
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getArchitecture
import com.davils.kreate.system.getOs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

public fun Project.initializeCInterop(extension: KreateExtension) {
    val cInteropConfig = extension.platform.multiplatform.cInterop
    if (!cInteropConfig.enabled.get()) return

    addCInteropTasks(extension)
    applyNativeTargets(cInteropConfig)
}

private fun Project.validateRootDir(rootDir: File) {
    if (!rootDir.exists()) {
        try {
            rootDir.mkdirs()
        } catch (e: Exception) {
            logger.error("Failed to create root directory: ${rootDir.absolutePath}", e)
        }
    }
}

private fun Project.addCInteropTasks(extension: KreateExtension) {
    val projectName = resolveProjectName(extension)

    val cInteropConfig = extension.platform.multiplatform.cInterop
    val projectRootDir = resolveRootDir(cInteropConfig)
    validateRootDir(projectRootDir)

    val rustProject = projectRootDir.resolve(projectName)
    val initializeRustProject by tasks.register<InitializeRustProject>("initializeRustProject") {
        this.workDir.set(projectRootDir)
        this.projectName.set(projectName)
    }

    val addRustDependencies by tasks.register<AddRustDependencies>("addRustDependencies") {
        workDir.set(rustProject)
        dependsOn(initializeRustProject)
    }

    val configureCargo by tasks.register<ConfigureCargo>("configureCargo") {
        workDir.set(rustProject)
        dependsOn(addRustDependencies)
    }

    val generateRustBuildScript by tasks.register<GenerateRustBuildScript>("generateRustBuildScript") {
        workDir.set(rustProject)
        this.projectName.set(projectName)
        dependsOn(configureCargo)
    }

    val compileRust by tasks.register<CompileRust>("compileRust") {
        workDir.set(rustProject)
        dependsOn(generateRustBuildScript)
    }

    val generateDefinitionFiles by tasks.register<GenerateDefinitionFiles>("generateDefinitionFiles") {
        workDir.set(rustProject)
        this.rootDir.set(projectRootDir)
        this.cInteropConfig.set(cInteropConfig)
        this.projectName.set(projectName)
        dependsOn(compileRust)
    }

    tasks.withType<CInteropProcess>().configureEach {
        dependsOn(generateDefinitionFiles)
    }
    executeTaskBeforeCompile(generateDefinitionFiles)
}

private fun Project.applyNativeTargets(cInteropConfig: CInteropExtension) {
    val arch by getArchitecture()
    val os by getOs()
    configure<KotlinMultiplatformExtension> {
        when (os) {
            OsTarget.WINDOWS -> {
                mingwX64 {
                    configureCInterop(this@applyNativeTargets)
                    cInteropConfig.mingwConfiguration.invoke(this)
                }
            }

            OsTarget.MACOS -> {
                macosArm64 {
                    configureCInterop(this@applyNativeTargets)
                    cInteropConfig.macosConfiguration.invoke(this)
                }
            }

            OsTarget.LINUX -> {
                when (arch) {
                    Architecture.X64 -> linuxX64 {
                        configureCInterop(this@applyNativeTargets)
                        cInteropConfig.linuxConfiguration.invoke(this)
                    }

                    else -> linuxArm64 {
                        configureCInterop(this@applyNativeTargets)
                        cInteropConfig.linuxConfiguration.invoke(this)
                    }
                }

            }

            else -> Unit
        }
    }
}
