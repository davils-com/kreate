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

package com.davils.kreate.cinterop.wiring

import com.davils.kreate.cinterop.CInteropExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

internal fun KotlinNativeTarget.configureCInterop(
    project: Project,
    cInteropConfig: CInteropExtension,
    projectName: String,
    projectRootDir: File
) {
    val defFiles = cInteropConfig.defFiles
    val definitionDirectory = projectRootDir.resolve(projectName).resolve(defFiles.dirName.get())
    val defFile = definitionDirectory.resolve(defFiles.fileName.get())
    val interopPackageName = cInteropConfig.packageNameOverride.orNull
        ?: "${project.group}.${projectName.lowercase()}.cinterop"

    compilations.all {
        cinterops {
            create(projectName) {
                packageName = interopPackageName
                definitionFile.set(defFile)
            }
        }
    }
}
