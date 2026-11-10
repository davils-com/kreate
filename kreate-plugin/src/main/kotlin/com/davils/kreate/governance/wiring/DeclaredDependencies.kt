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
package com.davils.kreate.governance.wiring

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.governance.policy.DeclaredDependency
import com.davils.kreate.governance.policy.RepositoryDeclaration
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

private const val PLUGIN_REPOSITORY_PREFIX: String = "__plugin_repository__"

internal fun Project.declaredDependencies(names: Set<String>): ListProperty<DeclaredDependency> {
    val merged = objects.listProperty(DeclaredDependency::class.java)
    checkedConfigurations(names).forEach { configuration ->
        val root = configuration.incoming.resolutionResult.rootComponent
        merged.addAll(root.map { component -> declaredOf(configuration.name, component) })
    }
    return merged
}

internal fun Project.forcedModules(names: Set<String>): Provider<List<ModuleCoordinate>> {
    val configurations = checkedConfigurations(names)
    return provider {
        configurations.flatMap { configuration -> configuration.resolutionStrategy.forcedModules.map(::coordinateOf) }
    }
}

internal fun Project.repositoryDeclarations(): Provider<List<RepositoryDeclaration>> {
    val buildscriptRepositories = buildscript.repositories.filterNot { repository -> repository.isPluginMirror() }
    val declared = repositories.toList() + buildscriptRepositories
    return provider { declared.map(::declarationOf) }
}

private fun ArtifactRepository.isPluginMirror(): Boolean = name.startsWith(PLUGIN_REPOSITORY_PREFIX)

private fun Project.checkedConfigurations(names: Set<String>): List<Configuration> {
    val selected = configurations.filter { configuration -> configuration.name in names }
    return selected.filter { configuration -> configuration.isCanBeResolved }
}

private fun declaredOf(configuration: String, root: ResolvedComponentResult): List<DeclaredDependency> {
    val direct = root.dependencies.filterNot { dependency -> dependency.isConstraint }
    return direct.mapNotNull { dependency -> declaredOf(configuration, dependency) }
}

private fun declaredOf(configuration: String, dependency: DependencyResult): DeclaredDependency? {
    val requested = dependency.requested as? ModuleComponentSelector ?: return null
    val constraint = requested.versionConstraint
    val candidates = listOf(constraint.strictVersion, constraint.requiredVersion, constraint.preferredVersion)
    val version = candidates.firstOrNull { candidate -> candidate.isNotEmpty() }.orEmpty()
    val resolved = dependency as? ResolvedDependencyResult
    val selected = resolved?.selected?.id
    if (selected != null && selected !is ModuleComponentIdentifier) return null
    val selectedVersion = (selected as? ModuleComponentIdentifier)?.version
    return DeclaredDependency(configuration, requested.group, requested.module, version, selectedVersion)
}

private fun coordinateOf(selector: ModuleVersionSelector): ModuleCoordinate =
    ModuleCoordinate(selector.group, selector.name, selector.version.orEmpty())

private fun declarationOf(repository: ArtifactRepository): RepositoryDeclaration {
    if (repository is MavenArtifactRepository) {
        return RepositoryDeclaration(repository.name, repository.url.toString(), repository.isAllowInsecureProtocol)
    }
    if (repository is IvyArtifactRepository) {
        return RepositoryDeclaration(repository.name, repository.url.toString(), repository.isAllowInsecureProtocol)
    }
    return RepositoryDeclaration(repository.name, null, false)
}
