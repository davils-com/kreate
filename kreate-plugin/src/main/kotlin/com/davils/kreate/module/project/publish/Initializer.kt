package com.davils.kreate.module.project.publish

import com.davils.kreate.KreateExtension
import org.gradle.api.Project

internal fun Project.initializePublish(extension: KreateExtension) {
    val publishConfig = extension.project.publish
    if (!publishConfig.enabled.get()) return

    val projectName = extension.project.name.orNull ?: project.name
    val projectDescription = extension.project.description.orNull

    configureMavenCentral(publishConfig, projectName, projectDescription)
    configureGitlab(publishConfig, projectName, projectDescription)
}
