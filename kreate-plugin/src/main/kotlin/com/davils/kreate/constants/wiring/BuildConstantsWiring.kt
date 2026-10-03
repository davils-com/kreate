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

package com.davils.kreate.constants.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.constants.BuildConstantsTaskNames
import com.davils.kreate.constants.task.GenerateBuildConstants
import com.davils.kreate.gradle.declaredProjectName
import com.davils.kreate.task.executeTaskBeforeCompile
import org.gradle.api.Project

private const val PACKAGE_SUFFIX = ".build"
private const val PACKAGE_SEPARATOR = '.'
private const val PATH_SEPARATOR = '/'

internal fun Project.initializeBuildConstants(extension: KreateExtension) {
    val buildConstantsExtension = extension.project.buildConstants
    if (!buildConstantsExtension.enabled.get()) return

    registerBuildConstantsTask(extension)
}

private fun Project.registerBuildConstantsTask(extension: KreateExtension) {
    val buildConstants = extension.project.buildConstants
    val platformExtension = extension.platform

    val constantsPath = buildConstants.path.get()
    val className = buildConstants.className.get()
    val resolvedPackageName = resolvePackageName(extension)

    val packagePath = resolvedPackageName.replace(oldChar = PACKAGE_SEPARATOR, newChar = PATH_SEPARATOR)
    val outputFile = layout.buildDirectory.file("$constantsPath/$packagePath/$className.kt")

    val constantsDir = layout.buildDirectory.dir(constantsPath).get().asFile
    addBuildConstantsToSourceSets(constantsDir.absolutePath)

    val task = tasks.register(BuildConstantsTaskNames.GENERATE, GenerateBuildConstants::class.java) {
        properties.set(buildConstants.getConstants())
        packageName.set(resolvedPackageName)
        this.className.set(className)
        explicitApi.set(platformExtension.explicitApi)
        file.set(outputFile)
    }

    executeTaskBeforeCompile(task)
}

private fun Project.resolvePackageName(extension: KreateExtension): String {
    val packageNameOverride = extension.project.buildConstants.packageNameOverride
    if (packageNameOverride.isPresent) return "${packageNameOverride.get()}$PACKAGE_SUFFIX"

    val packageName = "$group.${declaredProjectName(extension)}$PACKAGE_SUFFIX"
    return packageName.lowercase().replace(oldValue = " ", newValue = "")
}
