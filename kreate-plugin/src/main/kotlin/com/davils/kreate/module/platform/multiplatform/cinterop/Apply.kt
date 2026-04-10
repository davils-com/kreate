package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal fun KotlinNativeTarget.configureCInterop(project: Project) {
    val extension = project.extensions.getByType<KreateExtension>()
    val cInteropConfig = extension.platform.multiplatform.cInterop

    if (!cInteropConfig.enabled.get()) return

    val projectName = project.resolveProjectName(extension)
    val projectRootDir = project.resolveRootDir(cInteropConfig)
    val rustProject = projectRootDir.resolve(projectName)

    val defFile = rustProject.resolve(cInteropConfig.defFiles.dirName.get()).resolve(cInteropConfig.defFiles.fileName.get())

    val packageName = when (cInteropConfig.packageName.isPresent) {
        true -> cInteropConfig.packageName.get()
        false -> "${project.group}.${projectName.lowercase()}.cinterop"
    }

    compilations.all {
        cinterops {
            create(projectName) {
                this.packageName = packageName
                definitionFile.set(defFile)
            }
        }
    }
}
