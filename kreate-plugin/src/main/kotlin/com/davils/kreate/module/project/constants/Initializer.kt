package com.davils.kreate.module.project.constants

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

internal fun Project.initializeBuildConstants(extension: KreateExtension) {
    val buildConstantsExtension = extension.project.buildConstants
    if (!buildConstantsExtension.enabled.get()) {
        return
    }

    registerBuildConstantsTask(extension)
}