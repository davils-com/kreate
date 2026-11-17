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

package com.davils.kreate.floor.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import com.davils.kreate.floor.version.VersionFloorRule
import org.gradle.api.Project

private val TOOL_CONFIGURATION_PREFIXES: List<String> = listOf(
    "dokka",
    "detekt",
    "kover",
    "kotlinBouncyCastleConfiguration",
    "kotlinCompilerClasspath",
    "kotlinCompilerPluginClasspath",
    "kotlinBuildToolsApiClasspath",
    "kotlinKlibCommonizerClasspath",
    "kotlinNativeCompilerPluginClasspath"
)

internal object ToolClasspathsFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.LOCKING

    override fun apply(project: Project, extension: KreateExtension) {
        val floors = extension.project.toolClasspaths.declaredFloors()
        if (floors.isEmpty()) return
        val rules = floors.map { floor -> VersionFloorRule(floor.group, floor.version, floor.excluded) }
        val action = VersionFloorAction(rules)

        val toolConfigurations = project.configurations.matching { configuration ->
            TOOL_CONFIGURATION_PREFIXES.any { prefix -> configuration.name.startsWith(prefix) }
        }
        toolConfigurations.configureEach { resolutionStrategy.eachDependency(action) }
    }
}
