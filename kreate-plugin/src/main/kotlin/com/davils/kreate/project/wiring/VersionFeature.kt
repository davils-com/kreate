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
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import com.davils.kreate.settings.local.requestsLocalPublish
import org.gradle.api.Project

internal object VersionFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.VERSION

    override fun apply(project: Project, extension: KreateExtension) {
        val versionExtension = extension.project.version
        val isLocalPublish = isLocalPublish(project, extension)

        project.configureVersion(
            environmentVariable = versionExtension.environment.get(),
            propertyName = versionExtension.property.get(),
            localPublish = isLocalPublish
        )
        project.configureDescription(projectExtension = extension.project)
    }

    private fun isLocalPublish(project: Project, extension: KreateExtension): Boolean {
        val isLocalEnabled = extension.local.enabled.get()
        if (!isLocalEnabled) return false
        return requestsLocalPublish(project.gradle, project.providers)
    }
}
