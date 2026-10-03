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

import com.davils.kreate.KreateExtension
import com.davils.kreate.api.ApiValidationExtension
import com.davils.kreate.api.ApiValidationTaskNames
import com.davils.kreate.api.task.ApiCheck
import com.davils.kreate.api.task.ApiDump
import com.davils.kreate.gradle.GradlePluginId
import com.davils.kreate.gradle.KotlinPluginId
import com.davils.kreate.gradle.SourceSetName
import com.davils.kreate.gradle.qualifiedTaskPath
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private const val ACTUAL_DUMP_DIRECTORY = "kreate/api"

internal fun Project.initializeApiValidation(extension: KreateExtension) {
    val apiExtension = extension.project.apiValidation
    if (!apiExtension.enabled.get()) return
    if (validatesThroughKotlinPlugin(apiExtension)) {
        initializeKotlinAbiValidation(apiExtension)
        return
    }

    val classDirectories = collectMainClassDirectories()
    val validatedProjectPath = path
    val dumpFile = apiExtension.apiDirectory.file(apiExtension.dumpFileName)
    val dumpTaskPath = qualifiedTaskPath(ApiValidationTaskNames.DUMP)

    tasks.register<ApiDump>(ApiValidationTaskNames.DUMP) {
        this.classDirectories.from(classDirectories)
        applyFilters(apiExtension)
        this.dumpFile.set(dumpFile)
    }

    val checkTask = tasks.register<ApiCheck>(ApiValidationTaskNames.CHECK) {
        this.classDirectories.from(classDirectories)
        applyFilters(apiExtension)
        expectedDumpFile.from(dumpFile)
        this.dumpTaskPath.set(dumpTaskPath)
        projectPath.set(validatedProjectPath)
        actualDumpFile.set(
            layout.buildDirectory.file(
                apiExtension.dumpFileName.map { name -> "$ACTUAL_DUMP_DIRECTORY/$name" }
            )
        )
    }

    tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(checkTask)
    }
}

private fun ApiDump.applyFilters(extension: ApiValidationExtension) {
    nonPublicMarkers.set(extension.nonPublicMarkers)
    ignoredPackages.set(extension.ignoredPackages)
    ignoredClasses.set(extension.ignoredClasses)
}

private fun ApiCheck.applyFilters(extension: ApiValidationExtension) {
    nonPublicMarkers.set(extension.nonPublicMarkers)
    ignoredPackages.set(extension.ignoredPackages)
    ignoredClasses.set(extension.ignoredClasses)
}

private fun Project.collectMainClassDirectories(): ConfigurableFileCollection {
    val classDirectories = objects.fileCollection()

    plugins.withId(GradlePluginId.JAVA) {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        classDirectories.from(sourceSets.named(SourceSetName.MAIN).map { it.output.classesDirs })
    }

    plugins.withId(KotlinPluginId.MULTIPLATFORM) {
        val multiplatform = extensions.getByType<KotlinMultiplatformExtension>()
        multiplatform.targets.withType<KotlinJvmTarget>().configureEach {
            classDirectories.from(
                compilations.named(SourceSetName.MAIN).map { it.output.classesDirs }
            )
        }
    }

    return classDirectories
}
