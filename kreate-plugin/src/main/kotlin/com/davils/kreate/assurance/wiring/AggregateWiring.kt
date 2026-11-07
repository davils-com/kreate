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
package com.davils.kreate.assurance.wiring

import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.assurance.AssuranceTaskNames
import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.aggregate.AGGREGATE_FORMATS
import com.davils.kreate.assurance.task.AssuranceAggregateTask
import com.davils.kreate.assurance.task.AssuranceSummaryTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.register

private const val SUMMARY_TASK: String = "kreateAssuranceSummary"

private const val ELEMENTS: String = "kreateAssuranceElements"

private const val COLLECTED: String = "kreateAssuranceProjects"

private const val USAGE: String = "kreate-assurance"

private const val SUMMARY_FILE: String = "kreate/assurance/summary.json"

private const val AGGREGATE_DIRECTORY: String = "kreate/assurance/aggregate"

private const val ROOT_PATH: String = ":"

internal fun Project.registerAggregation(
    assurance: AssuranceExtension,
    configure: AssuranceSummaryTask.() -> Unit
) {
    val aggregate = assurance.aggregate
    if (!aggregate.enabled.get()) return
    val owner = this

    val summary = tasks.register<AssuranceSummaryTask>(SUMMARY_TASK) {
        configure()
        summaryFile.set(owner.layout.buildDirectory.file(SUMMARY_FILE))
    }
    configurations.consumable(ELEMENTS) {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, owner.objects.named(Usage::class.java, USAGE))
        outgoing.artifact(summary.flatMap { task -> task.summaryFile })
    }
    if (path != ROOT_PATH) return

    aggregate.members.convention(buildMembers())
    val collected = configurations.resolvable(COLLECTED) {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, owner.objects.named(Usage::class.java, USAGE))
    }
    val others = aggregate.members.map { paths -> paths.filter { member -> member != owner.path } }
    val projectDependencies = others.map { paths -> paths.map { member -> owner.projectDependency(member) } }
    collected.configure { dependencies.addAllLater(projectDependencies) }
    val files = collected.map { configuration -> configuration.incoming.artifactView { lenient(true) }.files }

    tasks.register<AssuranceAggregateTask>(AssuranceTaskNames.AGGREGATE) {
        summaries.from(files, summary.flatMap { task -> task.summaryFile })
        members.set(aggregate.members)
        formats.set(assurance.reports.formats.map(::aggregateFormatsOf))
        outputDirectory.set(owner.layout.buildDirectory.dir(AGGREGATE_DIRECTORY))
    }
}

private fun Project.projectDependency(member: String): Dependency = dependencies.project(mapOf("path" to member))

private fun aggregateFormatsOf(selected: Set<ReportFormat>): Set<ReportFormat> {
    val supported = selected.filter { format -> format in AGGREGATE_FORMATS }
    return supported.toSet() + ReportFormat.JSON
}
