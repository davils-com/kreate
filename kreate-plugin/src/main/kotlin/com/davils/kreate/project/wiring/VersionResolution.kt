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

package com.davils.kreate.project.wiring

import org.gradle.api.Project

private const val FALLBACK_VERSION: String = "1.0.0"

internal fun Project.getProjectVersion(environmentVariable: String, propertyName: String): String {
    val resolved = versionFromEnvironment(environmentVariable) ?: versionFromProperty(propertyName)
    if (resolved != null) return resolved

    logger.warn(
        "Kreate could not resolve a version for project '$path': environment variable " +
            "'$environmentVariable' is unset and project property '$propertyName' is missing or " +
            "'${Project.DEFAULT_VERSION}'. Falling back to $FALLBACK_VERSION."
    )
    return FALLBACK_VERSION
}

private fun Project.versionFromEnvironment(environmentVariable: String): String? {
    val value = providers.environmentVariable(environmentVariable).orNull
    return value?.takeIf { it.isNotBlank() }
}

private fun Project.versionFromProperty(propertyName: String): String? {
    val value = findProperty(propertyName)?.toString()
    val isAssigned = !value.isNullOrBlank() && value != Project.DEFAULT_VERSION
    if (!isAssigned) return null
    return value
}
