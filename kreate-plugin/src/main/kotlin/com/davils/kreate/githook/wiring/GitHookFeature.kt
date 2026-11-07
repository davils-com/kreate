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
package com.davils.kreate.githook.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.api.ApiValidationTaskNames
import com.davils.kreate.assurance.AssuranceTaskNames
import com.davils.kreate.changelog.ChangelogTaskNames
import com.davils.kreate.detekt.DetektTaskNames
import com.davils.kreate.feature.FeaturePhase
import com.davils.kreate.feature.KreateFeature
import com.davils.kreate.githook.GitHookTaskNames
import com.davils.kreate.githook.task.GitHookInstall
import com.davils.kreate.githook.task.GitHookUninstall
import com.davils.kreate.governance.GovernanceTaskNames
import com.davils.kreate.provenance.ProvenanceTaskNames
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

private const val ROOT_PATH: String = ":"

private val DEFAULT_TASKS: List<String> = listOf(
    DetektTaskNames.ANALYSE,
    ApiValidationTaskNames.CHECK,
    ChangelogTaskNames.CHECK,
    GovernanceTaskNames.POLICY,
    ProvenanceTaskNames.CONFUSION,
    AssuranceTaskNames.CONFIG_LINT
)

internal object GitHookFeature : KreateFeature {
    override val phase: FeaturePhase = FeaturePhase.ASSURANCE

    override fun apply(project: Project, extension: KreateExtension) {
        val hooks = extension.project.gitHooks
        if (!hooks.enabled.get()) return
        hooks.tasks.convention(DEFAULT_TASKS)

        val owner = project
        project.tasks.register(GitHookTaskNames.RUN) {
            group = KreateTaskGroup.KREATE.label
            description = "Runs the tasks of the pre-commit hook."
            dependsOn(hooks.tasks.map { names -> names.filter { name -> name in owner.tasks.names } })
        }
        if (project.path != ROOT_PATH) return

        project.tasks.register<GitHookInstall>(GitHookTaskNames.INSTALL) {
            projectDirectory.set(owner.layout.projectDirectory)
        }
        project.tasks.register<GitHookUninstall>(GitHookTaskNames.UNINSTALL) {
            projectDirectory.set(owner.layout.projectDirectory)
        }
    }
}
