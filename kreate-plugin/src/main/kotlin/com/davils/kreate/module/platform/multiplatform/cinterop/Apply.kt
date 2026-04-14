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

package com.davils.kreate.module.platform.multiplatform.cinterop

import com.davils.kreate.KreateExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Configures C-interop for a Kotlin native target.
 *
 * This function sets up the C-interop compilation, specifying the package name
 * and the definition file to be used, based on the Kreate configuration.
 *
 * @param project The Gradle project instance.
 * @since 1.0.0
 */
internal fun KotlinNativeTarget.configureCInterop(project: Project) {
    val extension = project.extensions.getByType<KreateExtension>()
    val cInteropConfig = extension.platform.multiplatform.cInterop

    if (!cInteropConfig.enabled.get()) return

    val projectName = project.resolveProjectName(extension)
    val projectRootDir = project.resolveRootDir(cInteropConfig)
    val rustProject = projectRootDir.resolve(projectName)

    val defFile = rustProject.resolve(cInteropConfig.defFiles.dirName.get()).resolve(cInteropConfig.defFiles.fileName.get())

    val packageName = when (cInteropConfig.packageNameOverride.isPresent) {
        true -> cInteropConfig.packageNameOverride.get()
        false -> "${project.group}.${projectName.lowercase()}.cinterop"
    }

    compilations.all {
        cinterops {
            create(projectName) {
                this.packageName = packageName
                definitionFile.set(defFile)
            }
        }
    }
}
