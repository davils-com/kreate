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

package com.davils.kreate.module.project.configuration

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.configuration.tasks.ConfigSchemaCheck
import com.davils.kreate.module.project.configuration.tasks.ConfigSchemaDump
import com.davils.kreate.module.project.configuration.tasks.ConfigValidate
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
private const val OUTCOME_FILE = "kreate/configuration/schema-check.txt"
private const val REPORT_FILE = "kreate/configuration/validation.txt"

/**
 * Initializes configuration schema export and checking for the project.
 *
 * Registers the dump, the check and the dry run, and wires the second and third into `check` - so
 * that a schema change which would break a deployed document and a repository whose own
 * configuration no longer loads are both found by an ordinary build rather than by a customer.
 *
 * @param extension The main Kreate extension.
 * @since 3.1.0
 */
internal fun Project.initializeConfigurationSchema(extension: KreateExtension) {
    val schemaExtension = extension.project.configurationSchema
    if (!schemaExtension.enabled.get()) return

    val runtimeClasspath = collectRuntimeClasspath()
    val declared = schemaExtension.definitions.get().map { definition -> definition.resolved() }
    val directory = schemaExtension.schemaDirectory
    // Captured here rather than read inside a task action: `path` inside a task configuration block
    // is the task's own path, and reaching for the project model at execution time is what makes a
    // task incompatible with the configuration cache.
    val dumpTaskPath = qualifiedTaskPath(KreateTasks.ConfigurationSchema.DUMP)

    tasks.register<ConfigSchemaDump>(KreateTasks.ConfigurationSchema.DUMP) {
        this.runtimeClasspath.from(runtimeClasspath)
        schemas.set(declared)
        schemaDirectory.set(directory)
        onlyIf { declared.isNotEmpty() }
    }

    val checkTask = tasks.register<ConfigSchemaCheck>(KreateTasks.ConfigurationSchema.CHECK) {
        this.runtimeClasspath.from(runtimeClasspath)
        schemas.set(declared)
        schemaDirectory.set(directory)
        expectedExports.from(directory.map { held -> held.asFileTree })
        this.dumpTaskPath.set(dumpTaskPath)
        outcomeFile.set(layout.buildDirectory.file(OUTCOME_FILE))
        onlyIf { declared.isNotEmpty() }
    }

    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(checkTask)
    }

    registerValidation(schemaExtension.validation.orNull?.resolved(), runtimeClasspath)
}

/**
 * Registers the dry run, when the project said where its reports come from.
 */
private fun Project.registerValidation(
    reports: DeclaredSchema?,
    runtimeClasspath: ConfigurableFileCollection
) {
    val declared = reports ?: return

    val validateTask = tasks.register<ConfigValidate>(KreateTasks.ConfigurationSchema.VALIDATE) {
        this.runtimeClasspath.from(runtimeClasspath)
        this.reports.set(declared)
        reportFile.set(layout.buildDirectory.file(REPORT_FILE))
    }

    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(validateTask)
    }
}

/**
 * The absolute path of one of this project's tasks, for a message somebody copies and runs.
 */
private fun Project.qualifiedTaskPath(name: String): String {
    if (path == Project.PATH_SEPARATOR) return "${Project.PATH_SEPARATOR}$name"

    return "$path${Project.PATH_SEPARATOR}$name"
}

/**
 * Collects the runtime classpath the project's own declarations can be read from.
 *
 * The compiled classes and everything they need, because reading a declaration means running the
 * code that builds it. Only JVM targets contribute: Kotlin/Native and JavaScript produce no classes
 * a reflective read could load.
 *
 * @return A file collection carrying the dependencies on the tasks that produce it.
 * @since 3.1.0
 */
private fun Project.collectRuntimeClasspath(): ConfigurableFileCollection {
    val classpath = objects.fileCollection()

    plugins.withId("java") {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        classpath.from(sourceSets.named(MAIN_SOURCE_SET_NAME).map { main -> main.runtimeClasspath })
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val multiplatform = extensions.getByType<KotlinMultiplatformExtension>()
        multiplatform.targets.withType<KotlinJvmTarget>().configureEach {
            val main = compilations.named(MAIN_SOURCE_SET_NAME)
            classpath.from(main.map { compilation -> compilation.output.allOutputs })
            classpath.from(main.map { compilation -> compilation.runtimeDependencyFiles })
        }
    }

    return classpath
}
