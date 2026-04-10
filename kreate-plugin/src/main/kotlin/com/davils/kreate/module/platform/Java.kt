package com.davils.kreate.module.platform

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

internal fun Project.configureJava(extension: KreateExtension) {
    val platformExtension = extension.platform

    configure<JavaPluginExtension> {
        val javaVersion = platformExtension.javaVersion.get()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}