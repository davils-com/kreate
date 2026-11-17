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

package com.davils.kreate.detekt.wiring

import com.davils.kreate.detekt.DetektExtension
import com.davils.kreate.detekt.DetektTaskNames
import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.api.tasks.TaskCollection
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import dev.detekt.gradle.extensions.DetektExtension as KDetektExtension

private const val DETEKT_PLUGINS_CONFIGURATION: String = "detektPlugins"

private const val LIBRARY_RULES: String = "dev.detekt:detekt-rules-libraries"

private const val DETEKT_TASK_PREFIX: String = "detekt"

private const val PLAIN_TASK_SUFFIX: String = "SourceSet"

internal fun Project.addLibraryRules(extension: DetektExtension) {
    if (!extension.libraryRules.get()) return
    val toolVersion = extensions.getByType<KDetektExtension>().toolVersion
    dependencies.addProvider(DETEKT_PLUGINS_CONFIGURATION, toolVersion.map { version -> "$LIBRARY_RULES:$version" })
}

internal fun Project.analyseTypesOnCheck(extension: DetektExtension) {
    if (!extension.typeResolution.get()) return
    val typeResolved = typeResolvedTasks()
    val check = tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }
    check.configureEach { dependsOn(typeResolved) }
    tasks.named(DetektTaskNames.ANALYSE) { dependsOn(typeResolved) }
}

internal fun Project.analysisTasks(): TaskCollection<Detekt> {
    val detektTasks = tasks.withType(Detekt::class.java)
    return detektTasks.matching { task -> task.name != DetektTaskNames.FORMAT_APPLY }
}

private fun Project.typeResolvedTasks(): TaskCollection<Detekt> {
    val detektTasks = tasks.withType(Detekt::class.java)
    return detektTasks.matching { task ->
        val name = task.name
        val isSourceSetTask = name.startsWith(DETEKT_TASK_PREFIX) && name != DETEKT_TASK_PREFIX
        isSourceSetTask && !name.endsWith(PLAIN_TASK_SUFFIX)
    }
}
