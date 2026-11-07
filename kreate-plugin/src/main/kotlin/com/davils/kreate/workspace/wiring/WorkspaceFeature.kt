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
package com.davils.kreate.workspace.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import org.gradle.api.Project

private const val ROOT_PATH: String = ":"

internal object WorkspaceFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.SCANNING

    override fun apply(project: Project, extension: KreateExtension) {
        val assurance = extension.assurance
        val workspace = assurance.workspace
        if (!assurance.enabled.get() || !workspace.enabled.get()) return

        project.registerWorkspaceMember(workspace)
        if (project.path == ROOT_PATH) project.registerWorkspaceRoot(workspace)
    }
}
