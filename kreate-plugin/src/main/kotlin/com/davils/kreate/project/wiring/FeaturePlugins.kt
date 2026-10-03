/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.project.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.project.ProjectExtension
import com.davils.kreate.publish.PublishExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

internal fun Project.applyDefaultGradlePlugins(projectExtension: ProjectExtension) {
    if (!projectExtension.applySerializationPlugin.get()) return

    pluginManager.apply(SerializationGradleSubplugin::class)
}

internal fun Project.applyFeaturePlugins(extension: KreateExtension) {
    val projectExtension = extension.project

    if (projectExtension.detekt.enabled.get()) pluginManager.apply(GradlePluginId.DETEKT)
    if (projectExtension.coverage.enabled.get()) pluginManager.apply(GradlePluginId.KOVER)
    if (projectExtension.benchmark.enabled.get()) pluginManager.apply(GradlePluginId.BENCHMARK)
    applyPublishingPlugins(projectExtension.publish)
}

private fun Project.applyPublishingPlugins(publish: PublishExtension) {
    if (!publish.enabled.get()) return

    pluginManager.apply(GradlePluginId.MAVEN_PUBLISH)

    val isMavenCentralEnabled = publish.repositories.mavenCentral.enabled.get()
    if (isMavenCentralEnabled) pluginManager.apply(GradlePluginId.MAVEN_CENTRAL)
}
