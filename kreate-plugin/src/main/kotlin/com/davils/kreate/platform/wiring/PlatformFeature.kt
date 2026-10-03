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

package com.davils.kreate.platform.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import com.davils.kreate.gradle.KotlinPluginId
import org.gradle.api.Project

internal object PlatformFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.PLATFORM

    override fun apply(project: Project, extension: KreateExtension) {
        val pluginManager = project.pluginManager
        if (pluginManager.hasPlugin(KotlinPluginId.MULTIPLATFORM)) applyMultiplatform(project, extension)
        if (pluginManager.hasPlugin(KotlinPluginId.JVM)) applyJvm(project, extension)
    }

    private fun applyMultiplatform(project: Project, extension: KreateExtension) {
        project.configureJava(extension)
        project.configureMultiplatformCompiler(extension)
    }

    private fun applyJvm(project: Project, extension: KreateExtension) {
        project.configureJava(extension)
        project.configureJvmCompiler(extension)
    }
}
