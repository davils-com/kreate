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

package com.davils.kreate.publish

import org.gradle.api.Action
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Maven publishing repositories.
 *
 * This extension provides nested configurations for GitLab, Maven Central and a Maven
 * repository with separate release and snapshot URLs.
 *
 * @since 1.0.0
 */
public abstract class MavenRepositoriesExtension @Inject constructor() {
    /**
     * Configuration for GitLab publishing.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val gitlab: GitLabExtension

    /**
     * Configuration for Maven Central publishing.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val mavenCentral: MavenCentralExtension

    /**
     * Configuration for publishing to a Maven repository that keeps releases and snapshots apart.
     * @since 4.2.0
     */
    @get:Nested
    public abstract val maven: MavenTargetExtension

    /**
     * Configures the [GitLabExtension] using the provided action and enables it.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun gitlab(action: Action<GitLabExtension>) {
        gitlab.enabled.set(true)
        action.execute(gitlab)
    }

    /**
     * Configures the [MavenCentralExtension] using the provided action and enables it.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun mavenCentral(action: Action<MavenCentralExtension>) {
        mavenCentral.enabled.set(true)
        action.execute(mavenCentral)
    }

    /**
     * Configures the [MavenTargetExtension] using the provided action and enables it.
     *
     * @param action The configuration action.
     * @since 4.2.0
     */
    public fun maven(action: Action<MavenTargetExtension>) {
        maven.enabled.set(true)
        action.execute(maven)
    }
}
