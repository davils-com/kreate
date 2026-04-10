package com.davils.kreate.module.platform.jvm

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.initializeJvmCompiler(extension: KreateExtension) {
    val platformExtension = extension.platform

    configure<KotlinJvmProjectExtension> {
        if (platformExtension.explicitApi.get()) {
            explicitApi()
        }

        compilerOptions {
            jvmToolchain(platformExtension.javaVersion.get().majorVersion.toInt())
            allWarningsAsErrors.set(platformExtension.allWarningsAsErrors.get())
        }
    }
}