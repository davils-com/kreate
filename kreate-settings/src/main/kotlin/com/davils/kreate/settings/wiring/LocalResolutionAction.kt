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
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import java.util.concurrent.TimeUnit

private const val COORDINATE_SEPARATOR: Char = ':'

private const val LOCAL_SNAPSHOT_CACHE_SECONDS: Int = 0

internal class LocalResolutionAction(
    private val substitutions: HashMap<String, String>,
    private val repositoryName: String,
    private val repositoryUri: String
) : IsolatedAction<Project> {
    override fun execute(target: Project) {
        target.repositories.maven(repositoryAction())
        target.configurations.configureEach(resolutionAction())
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

    private fun resolutionAction(): Action<Configuration> {
        val forced = substitutions.map { (coordinate, version) -> "$coordinate:$version" }

        val strategy = Action<ResolutionStrategy> {
            force(*forced.toTypedArray())
            cacheChangingModulesFor(LOCAL_SNAPSHOT_CACHE_SECONDS, TimeUnit.SECONDS)
        }

        return Action { resolutionStrategy(strategy) }
    }
}
