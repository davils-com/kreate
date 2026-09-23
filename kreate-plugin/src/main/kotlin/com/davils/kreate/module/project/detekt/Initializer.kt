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

package com.davils.kreate.module.project.detekt

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.project.detekt.extension.DetektExtension
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

/**
 * The suffix Detekt gives the task it registers for a Kotlin Multiplatform source set.
 *
 * Detekt registers two families of task on a multiplatform project: one per source set, without
 * type resolution, and one per compilation, with it. The source set family covers every file
 * exactly once, which is what makes it the right one to hang `check` on. The compilation family
 * overlaps - `commonMain` is part of every target's main compilation - and enables rules that need
 * type resolution, which is a decision about the rule set rather than about where analysis runs.
 */
private const val SOURCE_SET_TASK_SUFFIX: String = "SourceSet"

/**
 * The id of the Detekt plugin Kreate applies.
 *
 * @since 3.0.0
 */
internal const val DETEKT_PLUGIN_ID: String = "dev.detekt"

/**
 * Initializes the Detekt static analysis for the project.
 *
 * The plugin itself is applied earlier, by `applyFeaturePlugins`, so that every plugin behind an
 * enabled feature is in place before any feature starts configuring one.
 *
 * @param extension The main Kreate extension.
 * @since 1.2.0
 */
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

/**
 * Registers the one task that really analyses everything.
 *
 * Detekt's own aggregate `detekt` task has no sources on a multiplatform project - every file
 * belongs to a source set - so it succeeds having read nothing. Anything that ran it as a quality
 * gate would be green and blind, and the workaround every project reached for was to list the
 * per-source-set task names by hand in its pipeline. A list like that is wrong the day a target is
 * added and says nothing when it is.
 *
 * The matching collection is live: a source set registered later is picked up, which is what makes
 * this a computed list rather than a snapshot taken at the wrong moment.
 *
 * @return Unit
 * @since 3.0.0
 */
private fun Project.registerAnalyseTask() {
    tasks.register(KreateTasks.Detekt.ANALYSE) {
        group = KreateTasks.Detekt.GROUP
        description = "Runs Detekt over every source set that has sources."
        dependsOn(perSourceSetTasks())
    }
}

/**
 * Every Detekt task that belongs to a source set, as a live collection.
 *
 * @return The matching tasks.
 * @since 3.0.0
 */
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

/**
 * Makes `check` depend on the Detekt tasks that have something to analyse.
 *
 * Detekt's own plugin points `check` at the aggregate `detekt` task. Under the Kotlin JVM plugin
 * that task reads `src/main/kotlin` and `src/test/kotlin` and the arrangement is correct. Under the
 * multiplatform plugin it has no sources at all: every file belongs to a source set, and the
 * aggregate is left with nothing. It then succeeds without reading a line, which is the worst
 * possible outcome for a quality gate - the build stays green and the analysis silently stops
 * happening.
 *
 * The per-source-set tasks are added instead. On a project that has none, the collection is empty
 * and nothing changes.
 *
 * @since 2.3.0
 */
private fun Project.analyseOnCheck() {
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(perSourceSetTasks())
    }
}

/**
 * Resolves a report location into a directory named after the task that writes it.
 *
 * `build/reports/detekt/detekt.md` configured for a task called `detektJvmMainSourceSet` becomes
 * `build/reports/detekt/detektJvmMainSourceSet/detekt.md`. The file keeps the name it was given, so
 * anything collecting reports by extension still finds them, and a finding can be traced back to
 * the source set it was found in.
 *
 * @param location The configured report location.
 * @param taskName The name of the Detekt task the report belongs to.
 * @return The location, moved into a subdirectory named after the task.
 * @since 2.3.0
 */
private fun Project.perTaskReport(location: RegularFileProperty, taskName: String): Provider<RegularFile> =
    layout.file(location.map { report -> File(File(report.asFile.parentFile, taskName), report.asFile.name) })
