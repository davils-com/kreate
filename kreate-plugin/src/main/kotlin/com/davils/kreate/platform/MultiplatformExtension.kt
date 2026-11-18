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

package com.davils.kreate.platform

import com.davils.kreate.cinterop.CInteropExtension
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Kotlin Multiplatform settings.
 *
 * This extension provides the targets Kreate declares, where the Kotlin plugin finds the tools of
 * its JavaScript and Wasm targets, and nested configuration for C-interop.
 *
 * @since 1.0.0
 */
public abstract class MultiplatformExtension @Inject constructor() {
    /**
     * Configuration for C-interop.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val cInterop: CInteropExtension

    /**
     * The targets Kreate declares on the project.
     *
     * @since 5.1.0
     */
    @get:Nested
    public abstract val targets: MultiplatformTargetsExtension

    /**
     * Whether Node.js and Yarn for the JavaScript and Wasm targets come from the repositories of the
     * settings. Defaults to `false`.
     *
     * The Kotlin plugin adds a repository for each of them to the project, which a build with
     * `RepositoriesMode.FAIL_ON_PROJECT_REPOS` refuses. When this is `true`, Kreate stops the Kotlin
     * plugin from adding them, and the settings declare Ivy repositories that serve the modules
     * `org.nodejs:node` and `com.yarnpkg:yarn`. The root project of a multi-project build sets it as
     * well, because the Kotlin plugin installs the packages of every project there.
     *
     * @since 5.1.0
     */
    public abstract val toolsFromSettingsRepositories: Property<Boolean>

    /**
     * Configures the targets Kreate declares.
     *
     * @param action The configuration action.
     * @since 5.1.0
     */
    public fun targets(action: Action<MultiplatformTargetsExtension>) {
        action.execute(targets)
    }

    /**
     * Configures the [CInteropExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun cInterop(action: Action<CInteropExtension>) {
        action.execute(cInterop)
    }
}
