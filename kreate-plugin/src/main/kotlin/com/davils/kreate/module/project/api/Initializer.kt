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

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.api.extension.ApiValidationExtension
import com.davils.kreate.module.project.api.tasks.ApiCheck
import com.davils.kreate.module.project.api.tasks.ApiDump
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

private const val MAIN_SOURCE_SET_NAME = "main"

/**
 * Initializes binary compatibility validation for the project.
 *
 * Registers the dump and check tasks and wires the check into the `check` lifecycle task,
 * so that validation runs as part of an ordinary build rather than only when someone
 * remembers to ask for it.
 *
 * @param extension The main Kreate extension.
 * @since 2.1.0
 */
internal fun Project.initializeApiValidation(extension: KreateExtension) {
    val apiExtension = extension.project.apiValidation
    if (!apiExtension.enabled.get()) return
    if (validatesThroughKotlinPlugin(apiExtension)) {
        initializeKotlinAbiValidation(apiExtension)
        return
    }

    val classDirectories = collectMainClassDirectories()
    // Captured here on purpose: inside a task configuration block `path` is the task's own
    // path, which is how the failure message ended up naming the task as the project.
    val projectPath = path
    val dumpFile = apiExtension.apiDirectory.file(apiExtension.dumpFileName)
    val dumpTaskPath = if (projectPath == Project.PATH_SEPARATOR) {
        "${Project.PATH_SEPARATOR}${KreateTasks.ApiValidation.DUMP}"
    } else {
        "$projectPath${Project.PATH_SEPARATOR}${KreateTasks.ApiValidation.DUMP}"
    }

    tasks.register<ApiDump>(KreateTasks.ApiValidation.DUMP) {
        this.classDirectories.from(classDirectories)
        applyFilters(apiExtension)
        this.dumpFile.set(dumpFile)
    }

    val checkTask = tasks.register<ApiCheck>(KreateTasks.ApiValidation.CHECK) {
        this.classDirectories.from(classDirectories)
        applyFilters(apiExtension)
        expectedDumpFile.from(dumpFile)
        this.dumpTaskPath.set(dumpTaskPath)
        this.projectPath.set(projectPath)
        actualDumpFile.set(
            layout.buildDirectory.file(
                apiExtension.dumpFileName.map { name -> "kreate/api/$name" }
            )
        )
    }

    tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(checkTask)
    }
}

/**
 * Copies the shared filter settings from the extension onto a task.
 *
 * @param extension The API validation configuration.
 * @since 2.1.0
 */
private fun ApiDump.applyFilters(extension: ApiValidationExtension) {
    nonPublicMarkers.set(extension.nonPublicMarkers)
    ignoredPackages.set(extension.ignoredPackages)
    ignoredClasses.set(extension.ignoredClasses)
}

/**
 * Copies the shared filter settings from the extension onto a task.
 *
 * @param extension The API validation configuration.
 * @since 2.1.0
 */
private fun ApiCheck.applyFilters(extension: ApiValidationExtension) {
    nonPublicMarkers.set(extension.nonPublicMarkers)
    ignoredPackages.set(extension.ignoredPackages)
    ignoredClasses.set(extension.ignoredClasses)
}

/**
 * Collects the compiled main classes of whichever Kotlin or Java setup the project uses.
 *
 * The `java` branch covers Kotlin/JVM too, because the Kotlin JVM plugin applies `java`
 * and routes its output through the same source set. Only JVM targets contribute:
 * Kotlin/Native and JavaScript produce no class files, so there is nothing for ASM to
 * read.
 *
 * @return A file collection carrying the dependencies on the compile tasks that produce it.
 * @since 2.1.0
 */
private fun Project.collectMainClassDirectories(): ConfigurableFileCollection {
    val classDirectories = objects.fileCollection()

    plugins.withId("java") {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        classDirectories.from(sourceSets.named(MAIN_SOURCE_SET_NAME).map { it.output.classesDirs })
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val multiplatform = extensions.getByType<KotlinMultiplatformExtension>()
        multiplatform.targets.withType<KotlinJvmTarget>().configureEach {
            classDirectories.from(
                compilations.named(MAIN_SOURCE_SET_NAME).map { it.output.classesDirs }
            )
        }
    }

    return classDirectories
}
