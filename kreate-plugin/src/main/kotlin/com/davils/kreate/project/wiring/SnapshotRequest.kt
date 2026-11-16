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

import com.davils.kreate.project.ProjectVersionExtension
import org.gradle.api.Project

internal const val SNAPSHOT_PROPERTY: String = "kreate.snapshot"

internal const val SNAPSHOT_VARIABLE: String = "KREATE_SNAPSHOT"

private const val REQUESTED: String = "true"

internal fun Project.requestsSnapshot(versionExtension: ProjectVersionExtension): Boolean {
    val declared = versionExtension.snapshot.orNull
    if (declared != null) return declared

    val fromProperty = providers.gradleProperty(SNAPSHOT_PROPERTY)
    val fromVariable = providers.environmentVariable(SNAPSHOT_VARIABLE)
    val requested = fromProperty.orElse(fromVariable)
    val isRequested = requested.map { value -> value.equals(REQUESTED, ignoreCase = true) }
    return isRequested.getOrElse(false)
}
