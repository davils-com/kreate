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

import org.gradle.api.initialization.Settings
import org.gradle.vcs.VersionControlSpec
import org.gradle.vcs.git.GitVersionControlSpec
import java.net.URI

/**
 * Initializes Git source dependencies for the given settings.
 *
 * This function configures Gradle's `sourceControl` to include Git repositories
 * defined in the [KreateSettingsExtension].
 *
 * @param settings The Gradle [Settings] instance.
 * @param extension The [KreateSettingsExtension] containing Git dependency configurations.
 * @since 1.0.0
 */
internal fun initializeGitDependencies(settings: Settings, extension: KreateSettingsExtension) {
    settings.sourceControl {

    }
}
