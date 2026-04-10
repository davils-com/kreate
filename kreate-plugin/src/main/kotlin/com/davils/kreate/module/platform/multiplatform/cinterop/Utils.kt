package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import com.davils.kreate.system.Architecture
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getArchitecture
import com.davils.kreate.system.getOs
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import java.io.File

internal fun Project.resolveProjectName(extension: KreateExtension): String {
    val cInteropConfig = extension.platform.multiplatform.cInterop
    val name = when {
        cInteropConfig.nameOverride.isPresent -> cInteropConfig.nameOverride.get()
        extension.project.name.isPresent -> extension.project.name.get()
        else -> project.name
    }

    return name.lowercase().replace("-", "_")
}


internal fun Project.resolveRootDir(cInteropConfig: CInteropExtension): File {
    if (cInteropConfig.projectDirectory.isPresent) {
        return projectDir.resolve(cInteropConfig.projectDirectory.get().asFile)
    }
    return projectDir.resolve("cinterop")
}

internal fun resolveRustTargets(rustTargets: ListProperty<String>): List<String> {
    if (rustTargets.isPresent && rustTargets.get().isNotEmpty()) {
        return rustTargets.get()
    }

    val arch by getArchitecture()
    val os by getOs()

    return when (os) {
        OsTarget.WINDOWS -> listOf("x86_64-pc-windows-gnu")
        OsTarget.LINUX -> when (arch) {
            Architecture.X64 -> listOf("x86_64-unknown-linux-gnu")
            else -> listOf("aarch64-unknown-linux-gnu")
        }
        OsTarget.MACOS -> listOf("aarch64-apple-darwin")
        else -> throw GradleException("Unsupported OS: $os")
    }
}

internal fun resolveCargoCommand(): String {
    val os by getOs()
    return when (os) {
        OsTarget.MACOS -> "${System.getProperty("user.home")}/.cargo/bin/cargo"
        else -> "cargo"
    }
}