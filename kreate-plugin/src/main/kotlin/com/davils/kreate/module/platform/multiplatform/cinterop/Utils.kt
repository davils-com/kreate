package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
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