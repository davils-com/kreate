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

package com.davils.kreate.module.project.publish.extension

import com.davils.kreate.module.project.publish.extension.pom.PomExtension
import com.davils.kreate.module.project.publish.extension.repository.MavenRepositoriesExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring publishing settings in Kreate.
 *
 * This extension provides properties for inception year and website,
 * as well as nested configurations for POM metadata and target repositories.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class PublishExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether publishing is enabled.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The inception year of the project.
     * Defaults to 2024.
     * @since 1.0.0
     */
    public val inceptionYear: Property<Int> = factory.property(Int::class.java).convention(2024)

    /**
     * The official website URL of the project.
     * @since 1.0.0
     */
    public val website: Property<String> = factory.property(String::class.java)

    /**
     * Configuration for POM metadata.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val pom: PomExtension

    /**
     * Configuration for target repositories.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val repositories: MavenRepositoriesExtension

    /**
     * Configures the [PomExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun pom(action: Action<PomExtension>) {
        action.execute(pom)
    }

    /**
     * Configures the [MavenRepositoriesExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun repositories(action: Action<MavenRepositoriesExtension>) {
        action.execute(repositories)
    }
}
