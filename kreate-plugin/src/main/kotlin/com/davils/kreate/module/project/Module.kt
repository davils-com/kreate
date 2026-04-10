package com.davils.kreate.module.project

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import com.davils.kreate.module.project.constants.initializeBuildConstants
import org.gradle.api.Project

internal object ProjectModule : Module {
    override fun apply(project: Project, extension: KreateExtension) {
        project.configureCommon(extension)
    }

    private fun Project.configureCommon(extension: KreateExtension) {
        applyDefaultGradlePlugins()
        addRepositories()
        configureGroup()
        configureVersion()

        afterEvaluate {
            initializeProject(extension.project)
            initializeBuildConstants(extension)
        }
    }
}