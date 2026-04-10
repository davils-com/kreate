package com.davils.kreate.module.platform.multiplatform

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.initializeMultiplatformCompiler(extension: KreateExtension) {
    val platformConfig = extension.platform

    configure<KotlinMultiplatformExtension> {
        if (platformConfig.explicitApi.get()) {
            explicitApi()
        }

        compilerOptions {
            allWarningsAsErrors.set(platformConfig.allWarningsAsErrors.get())
        }
    }
}