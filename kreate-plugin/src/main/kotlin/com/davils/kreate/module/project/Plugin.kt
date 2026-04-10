package com.davils.kreate.module.project

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

internal fun Project.applyDefaultGradlePlugins() {
    pluginManager.apply(SerializationGradleSubplugin::class)
}
