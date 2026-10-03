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

package com.davils.kreate.coverage.wiring

import com.davils.kreate.coverage.CoverageAggregateExtension
import com.davils.kreate.gradle.GradlePluginId
import org.gradle.api.GradleException
import org.gradle.api.Project

private const val KOVER_CONFIGURATION: String = "kover"

internal fun Project.configureAggregation(extension: CoverageAggregateExtension) {
    if (!extension.enabled.get()) return

    aggregatedProjects(extension).forEach { target ->
        dependencies.add(KOVER_CONFIGURATION, target)
        applyKoverBeforeEvaluation(target)
    }
}

private fun Project.aggregatedProjects(extension: CoverageAggregateExtension): List<Project> {
    val configured = extension.projects.get()
    if (configured.isEmpty()) return subprojects.toList()

    return configured.map { projectPath -> requireAggregatedProject(projectPath) }
}

private fun Project.requireAggregatedProject(projectPath: String): Project =
    findProject(projectPath) ?: throw GradleException(
        "Kreate's coverage aggregation on project '$path' lists project '$projectPath', " +
            "which does not exist in this build."
    )

private fun applyKoverBeforeEvaluation(target: Project) {
    target.pluginManager.apply(GradlePluginId.KOVER)
}
