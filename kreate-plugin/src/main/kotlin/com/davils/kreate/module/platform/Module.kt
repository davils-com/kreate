package com.davils.kreate.module.platform

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

internal object PlatformModule : Module {
    override fun apply(project: Project, extension: KreateExtension) {
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.configureCommon(extension)
            project.configureMultiplatform(extension)
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.configureCommon(extension)
            project.configureJvm(extension)
        }
    }

    private fun Project.configureCommon(extension: KreateExtension) = afterEvaluate {
        configureJava(extension)
    }

    private fun Project.configureJvm(extension: KreateExtension): Unit = afterEvaluate {

    }

    private fun Project.configureMultiplatform(extension: KreateExtension): Unit = afterEvaluate {

    }
}