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

package com.davils.kreate.module

import org.gradle.api.Project

/**
 * Determines the version for a project.
 *
 * This function retrieves the version from an environment variable if it exists.
 * Otherwise, it attempts to find it in the project properties. If neither is
 * found or is "unspecified", it defaults to "1.0.0".
 *
 * @param env The environment variable name to check for the version.
 * @param prop The project property name to check for the version.
 * @return The resolved version string.
 * @since 1.0.0
 */
public fun Project.getProjectVersion(env: String, prop: String): String {
    val ciTag = System.getenv(env)
    if (ciTag != null) return ciTag

    val versionProp = findProperty(prop)?.toString()
    if (versionProp != null && versionProp != "unspecified") {
        return versionProp
    }

    return "1.0.0"
}
