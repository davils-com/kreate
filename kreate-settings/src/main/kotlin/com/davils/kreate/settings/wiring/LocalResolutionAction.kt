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

package com.davils.kreate.settings.wiring

import org.gradle.api.Action
import org.gradle.api.IsolatedAction
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySubstitution
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import java.util.concurrent.TimeUnit

private const val SUBSTITUTION_REASON: String = "Kreate local development mode"

private const val COORDINATE_SEPARATOR: Char = ':'

private const val LOCAL_SNAPSHOT_CACHE_SECONDS: Int = 0

internal class LocalResolutionAction(
    private val substitutions: HashMap<String, String>,
    private val repositoryName: String,
    private val repositoryUri: String
) : IsolatedAction<Project> {
    override fun execute(target: Project) {
        target.repositories.maven(repositoryAction())
        target.configurations.configureEach(substitutionAction())
    }

    private fun repositoryAction(): Action<MavenArtifactRepository> {
        val coordinates = substitutions.keys
            .map { key -> key.substringBefore(COORDINATE_SEPARATOR) to key.substringAfter(COORDINATE_SEPARATOR) }
            .filter { (group, name) -> group.isNotEmpty() && name.isNotEmpty() }

        val snapshotsOnly = Action<MavenRepositoryContentDescriptor> { snapshotsOnly() }
        val onlyPublished = Action<RepositoryContentDescriptor> {
            coordinates.forEach { (group, name) -> includeModule(group, name) }
        }

        return Action {
            name = repositoryName
            setUrl(repositoryUri)
            mavenContent(snapshotsOnly)
            content(onlyPublished)
        }
    }

    private fun substitutionAction(): Action<Configuration> {
        val forced = substitutions.map { (coordinate, version) -> "$coordinate:$version" }
        val versions = substitutions

        val substitute = Action<DependencySubstitution> { substituteLocalVersion(versions) }
        val allSubstitutions = Action<DependencySubstitutions> { all(substitute) }

        val strategy = Action<ResolutionStrategy> {
            dependencySubstitution(allSubstitutions)
            force(*forced.toTypedArray())
            cacheChangingModulesFor(LOCAL_SNAPSHOT_CACHE_SECONDS, TimeUnit.SECONDS)
        }

        return Action { resolutionStrategy(strategy) }
    }
}

private fun DependencySubstitution.substituteLocalVersion(versions: Map<String, String>) {
    val selector = requested as? ModuleComponentSelector ?: return
    val coordinate = "${selector.group}$COORDINATE_SEPARATOR${selector.module}"
    val version = versions[coordinate] ?: return
    if (selector.version == version) return

    useTarget("$coordinate$COORDINATE_SEPARATOR$version", SUBSTITUTION_REASON)
}
