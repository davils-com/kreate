package com.davils.kreate.module.project

import com.davils.kreate.Davils
import com.davils.kreate.module.getProjectVersion
import org.gradle.api.Project

internal fun Project.configureGroup() {
    group = Davils.Organization.GROUP
}

internal fun Project.configureVersion() {
    val projectVersion = getProjectVersion()
    if (projectVersion != version.toString()) {
        version = projectVersion
    }
}

internal fun Project.initializeProject(projectExtension: ProjectExtension) {
    description = projectExtension.description.get()
}