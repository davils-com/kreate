package com.davils.kreate.module.project.docs

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.DokkaPlugin

internal fun Project.applyDokkaPlugin() {
    pluginManager.apply(DokkaPlugin::class)
}