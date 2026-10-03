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

package com.davils.kreate.configuration.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.configuration.ConfigurationSchemaTaskNames
import com.davils.kreate.configuration.schema.DeclaredSchema
import com.davils.kreate.configuration.schema.resolved
import com.davils.kreate.configuration.task.ConfigSchemaCheck
import com.davils.kreate.configuration.task.ConfigSchemaDump
import com.davils.kreate.configuration.task.ConfigValidate
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

private const val OUTCOME_FILE = "kreate/configuration/schema-check.txt"
private const val REPORT_FILE = "kreate/configuration/validation.txt"

internal fun Project.initializeConfigurationSchema(extension: KreateExtension) {
    val schemaExtension = extension.project.configurationSchema
    if (!schemaExtension.enabled.get()) return

    val runtimeClasspath = collectRuntimeClasspath()
    val declared = schemaExtension.definitions.get().map { definition -> definition.resolved() }
    val directory = schemaExtension.schemaDirectory
    val dumpTaskPath = qualifiedTaskPath(ConfigurationSchemaTaskNames.DUMP)

    tasks.register<ConfigSchemaDump>(ConfigurationSchemaTaskNames.DUMP) {
        this.runtimeClasspath.from(runtimeClasspath)
        schemas.set(declared)
        schemaDirectory.set(directory)
        onlyIf { declared.isNotEmpty() }
    }

    val checkTask = tasks.register<ConfigSchemaCheck>(ConfigurationSchemaTaskNames.CHECK) {
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

private fun Project.registerValidation(
    reports: DeclaredSchema?,
    runtimeClasspath: ConfigurableFileCollection
) {
    val declared = reports ?: return

    val validateTask = tasks.register<ConfigValidate>(ConfigurationSchemaTaskNames.VALIDATE) {
        this.runtimeClasspath.from(runtimeClasspath)
        this.reports.set(declared)
        reportFile.set(layout.buildDirectory.file(REPORT_FILE))
    }

    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(validateTask)
    }
}

private fun Project.collectRuntimeClasspath(): ConfigurableFileCollection {
    val classpath = objects.fileCollection()

    plugins.withId(GradlePluginId.JAVA) {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        classpath.from(sourceSets.named(SourceSetName.MAIN).map { main -> main.runtimeClasspath })
    }

    plugins.withId(KotlinPluginId.MULTIPLATFORM) {
        val multiplatform = extensions.getByType<KotlinMultiplatformExtension>()
        multiplatform.targets.withType<KotlinJvmTarget>().configureEach {
            val main = compilations.named(SourceSetName.MAIN)
            classpath.from(main.map { compilation -> compilation.output.allOutputs })
            classpath.from(main.map { compilation -> compilation.runtimeDependencyFiles })
        }
    }

    return classpath
}
