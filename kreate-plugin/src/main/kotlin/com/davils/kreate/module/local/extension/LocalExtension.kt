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

package com.davils.kreate.module.local.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for the local development workflow.
 *
 * Before 3.2.0 a fix in one Davils library could only be tried in another by tagging a release
 * and waiting for a pipeline to push it to a registry — which made the cost of testing a one line
 * change the same as the cost of shipping one. This feature closes that loop: a producer installs
 * itself into the local Maven repository at a snapshot version, and consumers substitute it in
 * without a single file in either repository being edited.
 *
 * The feature has two halves and needs both. This extension owns the producer half — publishing
 * and the tasks that manage it. Resolution is owned by the separate `com.davils.kreate.settings`
 * plugin, because substitution has to be installed before any configuration resolves and has to
 * reach `build-logic`, which does not apply this plugin.
 *
 * Enabling it registers tasks; it does not by itself change how anything resolves. What switches
 * resolution over is the act of publishing, which is the only sequence that cannot be got wrong
 * by forgetting a flag.
 *
 * @param factory The object factory used for creating properties.
 * @since 3.2.0
 */
public abstract class LocalExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 3.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether the local development tasks are registered for this project.
     *
     * Defaults to `true`. Unlike most Kreate features this one is on by default, because the
     * tasks it adds are inert: they do nothing until a developer runs one by name, and they
     * refuse to run in CI at all. An opt-in switch would mostly serve to make the feature
     * undiscoverable in the repositories that need it.
     *
     * @since 3.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The version suffix a local publication carries.
     *
     * Defaults to `-SNAPSHOT`, and there is rarely a reason to change it. Both Maven and Gradle
     * give this suffix its own resolution semantics, and the consumer side declares the local
     * repository with `mavenContent { snapshotsOnly() }` — which is what guarantees a local
     * build can never be mistaken for the release of the same version.
     *
     * @since 3.2.0
     */
    public val snapshotSuffix: Property<String> =
        factory.property(String::class.java).convention("-SNAPSHOT")

    /**
     * The environment variables whose presence means the build is running in CI.
     *
     * Defaults to `CI`, `GITLAB_CI`, `GITHUB_ACTIONS` and `CI_PIPELINE_ID`. Local mode is always
     * off when one of them is set, and a runner that additionally carries local state fails the
     * build rather than resolving from it.
     *
     * @since 3.2.0
     */
    public val ciEnvironmentVariables: ListProperty<String> = factory
        .listProperty(String::class.java)
        .convention(listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID"))

    /**
     * Configuration for orchestrating local publishes across several repositories.
     *
     * @since 3.2.0
     */
    @get:Nested
    public abstract val workspace: LocalWorkspaceExtension

    /**
     * Configures the [LocalWorkspaceExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 3.2.0
     */
    public fun workspace(action: Action<LocalWorkspaceExtension>) {
        action.execute(workspace)
    }
}
