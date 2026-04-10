package com.davils.kreate.module.project.tests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin
import io.kotest.framework.gradle.KotestPlugin
import com.google.devtools.ksp.gradle.KspGradleSubplugin

internal fun Project.validateKotestPlugin() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        if (!plugins.hasPlugin(KspGradleSubplugin::class)) {
            throw IllegalStateException("You need to apply the 'ksp' plugin by yourself in multiplatform projects if you want to use kotest.")
        }

        if (!plugins.hasPlugin(KotestPlugin::class)) {
            throw IllegalStateException("You need to apply the 'kotest' plugin by yourself in multiplatform projects if you want to use kotest.")
        }
    }
}