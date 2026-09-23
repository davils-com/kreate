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

package com.davils.kreate.settings

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

/*
 * Every Gradle call in this file is spelled out as an `Action` rather than a trailing lambda.
 * `RepositoryHandler`, `ArtifactRepository` and `ResolutionStrategy` all declare Groovy `Closure`
 * overloads alongside their `Action` ones, and outside a Kotlin build script the `Closure` overload
 * is the one that wins — with an error message that names neither the cause nor the remedy. The
 * same note already sits on `gitlabPackageRegistry`, for the same reason.
 */

/**
 * The reason recorded against every substitution, shown by `dependencyInsight`.
 *
 * @since 3.2.0
 */
private const val SUBSTITUTION_REASON: String = "Kreate local development mode"

/**
 * Points a project at the local Maven repository for exactly the coordinates published there.
 *
 * Installed through `GradleLifecycle.beforeProject`, which runs before a project's build script.
 * That matters twice over: the repository lands ahead of the ones the script declares, and the
 * resolution strategy is in place before anything can resolve.
 *
 * An [IsolatedAction] is `Serializable`, so this captures nothing but plain data. That is also
 * what makes it configuration cache correct — the alternative, a lambda closing over the settings
 * object, is precisely what the cache refuses to store.
 *
 * @since 3.2.0
 */
internal class LocalResolutionAction(
    /**
     * The version to resolve, keyed by `group:name`.
     * @since 3.2.0
     */
    private val substitutions: HashMap<String, String>,
    /**
     * The name the injected repository is reported under.
     * @since 3.2.0
     */
    private val repositoryName: String,
    /**
     * The URI of the local Maven repository.
     * @since 3.2.0
     */
    private val repositoryUri: String
) : IsolatedAction<Project> {
    /**
     * Applies the repository and the substitutions to a project.
     *
     * @param target The project being configured.
     * @since 3.2.0
     */
    override fun execute(target: Project) {
        target.repositories.maven(repositoryAction())
        target.configurations.configureEach(substitutionAction())
    }

    /**
     * Declares the local Maven repository, restricted to what is actually published there.
     *
     * Two filters, and both are load bearing:
     *
     * - `snapshotsOnly()` means a release can never be served from here, whatever ends up in the
     *   directory. This is what makes `-SNAPSHOT` more than a naming convention.
     * - `includeModule` per coordinate means the repository is consulted only for artefacts a
     *   `kreateLocalPublish` actually installed. A sibling module of the same library that was
     *   not published still resolves from the registry, and no request for an unrelated artefact
     *   is ever answered — or even attempted — here.
     *
     * Declared explicitly rather than through `mavenLocal()` so that it honours the same
     * `maven.repo.local` resolution the publishing side used. `mavenLocal()` consults the system
     * properties of the daemon it happens to run in, which need not be the one that published.
     *
     * @return The action that declares the repository.
     * @since 3.2.0
     */
    private fun repositoryAction(): Action<MavenArtifactRepository> {
        val coordinates = substitutions.keys
            .map { key -> key.substringBefore(':') to key.substringAfter(':') }
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

    /**
     * Rewrites every request for a locally published coordinate onto the local version.
     *
     * Substitution rather than `eachDependency { useVersion(...) }`, and the difference is not
     * stylistic. A library published as a set of modules is normally consumed through a BOM: the
     * module is requested without a version and a platform contributes the constraint.
     * `useVersion` applies before conflict resolution, which then compares `1.1.0-SNAPSHOT`
     * against the BOM's `1.1.0` and prefers the release — silently undoing the substitution.
     * `useTarget` rewrites the selector itself, so there is no later comparison to lose.
     *
     * [ResolutionStrategy.force] is applied to the same coordinates as a second line: it wins
     * outright if a constraint ever reaches conflict resolution by a route substitution missed.
     *
     * @return The action applied to every configuration.
     * @since 3.2.0
     */
    private fun substitutionAction(): Action<Configuration> {
        val forced = substitutions.map { (coordinate, version) -> "$coordinate:$version" }
        val local = substitutions

        val substitute = Action<DependencySubstitution> {
            val selector = requested as? ModuleComponentSelector
            val version = selector?.let { local["${it.group}:${it.module}"] }

            if (selector != null && version != null && selector.version != version) {
                useTarget("${selector.group}:${selector.module}:$version", SUBSTITUTION_REASON)
            }
        }
        val substitutions = Action<DependencySubstitutions> { all(substitute) }

        val strategy = Action<ResolutionStrategy> {
            dependencySubstitution(substitutions)
            force(*forced.toTypedArray())

            // A locally published snapshot is replaced in place on every iteration, so a cached
            // answer is a stale one by definition.
            cacheChangingModulesFor(0, TimeUnit.SECONDS)
        }

        return Action { resolutionStrategy(strategy) }
    }
}

/**
 * Turns dependency locking off for every configuration of a project.
 *
 * Registered through `afterProject` rather than `beforeProject`. `lockAllConfigurations()` is a
 * `configureEach { activateDependencyLocking() }` — confirmed in Gradle 9.6's bytecode — and
 * `configureEach` actions run in registration order, so a deactivation registered before the
 * build script would run first and the script's activation would win.
 *
 * That argument is why `afterProject` was chosen, but it could not be demonstrated: Gradle 9.6
 * was not observed to enforce lock state against a selector that substitution had already
 * rewritten, so `BuildLogicLocalFunctionalTest` stays green with either hook, with `force`
 * removed, and with this action removed entirely.
 *
 * It stays because it is right on its own terms — a lock file records released versions and
 * cannot contain a machine-local snapshot, so enforcing it against one would be enforcing a
 * constraint that can never be satisfied. Treat it as a precaution against a Gradle version that
 * does check, not as something the suite is guarding.
 *
 * @since 3.2.0
 */
internal class DeactivateLockingAction : IsolatedAction<Project> {
    /**
     * Deactivates locking on the project's configurations.
     *
     * @param target The project being configured.
     * @since 3.2.0
     */
    override fun execute(target: Project) {
        target.configurations.configureEach(
            Action<Configuration> { resolutionStrategy.deactivateDependencyLocking() }
        )
    }
}
