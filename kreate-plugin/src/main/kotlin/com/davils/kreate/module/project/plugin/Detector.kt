package com.davils.kreate.module.project.plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper

internal fun Project.isMultiplatform(): Boolean = plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class)
