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

package com.davils.kreate.module.platform

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Resolves the feature project name shared by platform features such as C-interop and JNI.
 *
 * It uses the given feature-level override if present, otherwise it falls back to the
 * Kreate project name and finally to the Gradle project name. The returned value is
 * lowercased and has hyphens replaced with underscores to be safe across native tooling
 * (Cargo, CMake, JNI symbol names).
 *
 * @param extension The Kreate configuration extension.
 * @param nameOverride The feature-specific name override property.
 * @return The resolved and sanitized project name.
 * @since 1.1.0
 */
internal fun Project.resolveFeatureProjectName(
    extension: KreateExtension,
    nameOverride: Property<String>
): String {
    val name = when {
        nameOverride.isPresent -> nameOverride.get()
        extension.project.name.isPresent -> extension.project.name.get()
        else -> project.name
    }
    return name.lowercase().replace("-", "_")
}
