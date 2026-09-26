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

package com.davils.kreate.module.project.api

import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.api.extension.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension as KotlinAbiValidationExtension

private const val MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
private const val SUBPACKAGE_WILDCARD = ".**"

/**
 * Whether this project's validation is handed to the Kotlin Gradle plugin.
 *
 * @param extension The API validation configuration.
 * @return `true` when [ApiValidationExtension.klib] is on and the project is multiplatform.
 * @since 3.5.0
 */
internal fun Project.validatesThroughKotlinPlugin(extension: ApiValidationExtension): Boolean =
    extension.klib.get() && plugins.hasPlugin(MULTIPLATFORM_PLUGIN_ID)

/**
 * Hands validation to the ABI validation built into the Kotlin Gradle plugin.
 *
 * The Kotlin plugin reads class files and klibs alike, so every target of a multiplatform
 * project is covered, including the Wasm, JavaScript, Native and Android library targets that
 * Kreate's class file reader cannot see. Kreate's filters and dump directory are carried over,
 * and the Kreate task names stay the entry points, so a build script and a CI job do not change
 * when a project opts in.
 *
 * @param extension The API validation configuration.
 * @since 3.5.0
 */
@OptIn(ExperimentalAbiValidation::class)
internal fun Project.initializeKotlinAbiValidation(extension: ApiValidationExtension) {
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
    kotlin.abiValidation { configureFrom(extension) }

    val kotlinValidation = kotlin.abiValidation
    registerDelegate(
        KreateTasks.ApiValidation.DUMP,
        "Writes the public ABI of every target of this project to its checked-in dumps.",
        kotlinValidation.updateTaskProvider
    )
    val checkTask = registerDelegate(
        KreateTasks.ApiValidation.CHECK,
        "Verifies that the public ABI of every target matches the checked-in dumps.",
        kotlinValidation.checkTaskProvider
    )

    tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(checkTask)
    }
}

/**
 * Carries Kreate's dump directory and filters over to the Kotlin plugin's validation.
 *
 * An ignored package becomes a `package.**` name filter, which is how the Kotlin plugin spells
 * "this package and everything below it".
 *
 * @param extension The API validation configuration.
 * @since 3.5.0
 */
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

/**
 * Registers a Kreate task that runs one of the Kotlin plugin's validation tasks.
 *
 * @param name The Kreate task name.
 * @param taskDescription What the task does, as shown by `gradle tasks`.
 * @param delegate The Kotlin plugin task that does the work.
 * @return The registered task.
 * @since 3.5.0
 */
private fun Project.registerDelegate(
    name: String,
    taskDescription: String,
    delegate: TaskProvider<Task>
): TaskProvider<Task> = tasks.register(name) {
    group = KreateTasks.ApiValidation.GROUP
    description = taskDescription
    dependsOn(delegate)
}
