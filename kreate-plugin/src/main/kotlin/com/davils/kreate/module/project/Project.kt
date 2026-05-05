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

package com.davils.kreate.module.project

import com.davils.kreate.module.getProjectVersion
import org.gradle.api.Project

/**
 * Configures the project version based on environment variables or properties.
 *
 * @param env The environment variable name to check.
 * @param prop The project property name to check.
 * @since 1.0.0
 */
internal fun Project.configureVersion(env: String, prop: String) {
    val projectVersion = getProjectVersion(env, prop)
    if (projectVersion != version.toString()) {
        version = projectVersion
    }
}

/**
 * Initializes project basic settings from the extension.
 *
 * @param projectExtension The project extension containing configuration.
 * @since 1.0.0
 */
internal fun Project.initializeProject(projectExtension: ProjectExtension) {
    description = projectExtension.description.get()
}
