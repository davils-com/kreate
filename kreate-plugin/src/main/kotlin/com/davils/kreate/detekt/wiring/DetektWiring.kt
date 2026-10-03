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

import com.davils.kreate.KreateExtension
import com.davils.kreate.detekt.DetektExtension
import com.davils.kreate.detekt.DetektTaskNames
import com.davils.kreate.task.KreateTaskGroup
import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import dev.detekt.gradle.extensions.DetektExtension as KDetektExtension

private const val SOURCE_SET_TASK_SUFFIX: String = "SourceSet"

internal fun Project.initializeDetekt(extension: KreateExtension) {
    val detektExtension = extension.project.detekt
    if (!detektExtension.enabled.get()) {
        return
    }

    configureDetektExtension(detektExtension)
    configureDetektTasks(detektExtension)
    addKreateRuleSet(detektExtension)
    registerAnalyseTask()
    analyseOnCheck()
}

private fun Project.registerAnalyseTask() {
    tasks.register(DetektTaskNames.ANALYSE) {
        group = KreateTaskGroup.DETEKT.label
        description = "Runs Detekt over every source set that has sources."
        dependsOn(perSourceSetTasks())
    }
}

private fun Project.perSourceSetTasks() = tasks.withType(Detekt::class.java).matching { task ->
    task.name.endsWith(SOURCE_SET_TASK_SUFFIX)
}

private fun Project.configureDetektExtension(extension: DetektExtension) {
    extensions.configure<KDetektExtension> {
        config.setFrom(files(extension.config))
        buildUponDefaultConfig.set(extension.buildUponDefaultConfig)
        allRules.set(extension.allRules)
    }
}

private fun Project.configureDetektTasks(extension: DetektExtension) {
    val generated = UnderDirectory(layout.buildDirectory.get().asFile.absolutePath)

    tasks.withType<Detekt>().configureEach {
        exclude(generated)

        reports {
            checkstyle {
                required.set(extension.reports.checkstyle.required)
                outputLocation.set(perTaskReport(extension.reports.checkstyle.outputLocation, name))
            }

            html {
                required.set(extension.reports.html.required)
                outputLocation.set(perTaskReport(extension.reports.html.outputLocation, name))
            }

            markdown {
                required.set(extension.reports.markdown.required)
                outputLocation.set(perTaskReport(extension.reports.markdown.outputLocation, name))
            }

            sarif {
                required.set(extension.reports.sarif.required)
                outputLocation.set(perTaskReport(extension.reports.sarif.outputLocation, name))
            }
        }
    }
}

private fun Project.analyseOnCheck() {
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(perSourceSetTasks())
    }
}

private fun Project.perTaskReport(location: RegularFileProperty, taskName: String): Provider<RegularFile> =
    layout.file(location.map { report -> File(File(report.asFile.parentFile, taskName), report.asFile.name) })
