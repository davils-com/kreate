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

package com.davils.kreate

import com.davils.kreate.module.platform.PlatformExtension
import com.davils.kreate.module.project.ProjectExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * The main extension class for configuring the Kreate plugin.
 *
 * This class provides access to project-level and platform-level configurations.
 *
 * @param factory The [ObjectFactory] used to create instances of configuration objects.
 * @since 1.0.0
 */
public abstract class KreateExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Configuration for project-level settings.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val project: ProjectExtension

    /**
     * Configuration for platform-level settings.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val platform: PlatformExtension

    /**
     * Configures the project-level settings.
     *
     * @param action The configuration action for [ProjectExtension].
     * @since 1.0.0
     */
    public fun project(action: Action<ProjectExtension>) {
        action.execute(project)
    }

    /**
     * Configures the platform-level settings.
     *
     * @param action The configuration action for [PlatformExtension].
     * @since 1.0.0
     */
    public fun platform(action: Action<PlatformExtension>) {
        action.execute(platform)
    }
}