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

package com.davils.kreate.api.wiring

import com.davils.kreate.api.ApiValidationExtension
import com.davils.kreate.api.ApiValidationTaskNames
import com.davils.kreate.gradle.KotlinPluginId
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension as KotlinAbiValidationExtension

private const val SUBPACKAGE_WILDCARD = ".**"

internal fun Project.validatesThroughKotlinPlugin(extension: ApiValidationExtension): Boolean =
    extension.klib.get() && plugins.hasPlugin(KotlinPluginId.MULTIPLATFORM)

@OptIn(ExperimentalAbiValidation::class)
internal fun Project.initializeKotlinAbiValidation(extension: ApiValidationExtension) {
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
    kotlin.abiValidation { configureFrom(extension) }

    val kotlinValidation = kotlin.abiValidation
    registerDelegate(
        ApiValidationTaskNames.DUMP,
        "Writes the public ABI of every target of this project to its checked-in dumps.",
        kotlinValidation.updateTaskProvider
    )
    val checkTask = registerDelegate(
        ApiValidationTaskNames.CHECK,
        "Verifies that the public ABI of every target matches the checked-in dumps.",
        kotlinValidation.checkTaskProvider
    )

    tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(checkTask)
    }
}

@OptIn(ExperimentalAbiValidation::class)
private fun KotlinAbiValidationExtension.configureFrom(extension: ApiValidationExtension) {
    referenceDumpDir.set(extension.apiDirectory)

    val ignoredPackagePatterns = extension.ignoredPackages.map { packages ->
        packages.map { name -> name + SUBPACKAGE_WILDCARD }
    }
    filters.exclude {
        annotatedWith.addAll(extension.nonPublicMarkers)
        byNames.addAll(extension.ignoredClasses)
        byNames.addAll(ignoredPackagePatterns)
    }
}

private fun Project.registerDelegate(
    name: String,
    taskDescription: String,
    delegate: TaskProvider<Task>
): TaskProvider<Task> = tasks.register(name) {
    group = KreateTaskGroup.API.label
    description = taskDescription
    dependsOn(delegate)
}
