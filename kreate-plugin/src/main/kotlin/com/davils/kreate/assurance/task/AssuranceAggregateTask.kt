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
package com.davils.kreate.assurance.task

import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.aggregate.AGGREGATE_FORMATS
import com.davils.kreate.assurance.aggregate.AggregateReport
import com.davils.kreate.assurance.aggregate.AggregateWriters
import com.davils.kreate.assurance.aggregate.ProjectSummaryJson
import com.davils.kreate.assurance.policy.GateStatus
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Instant

@DisableCachingByDefault(because = "The aggregation repeats the verdicts of the day")
internal abstract class AssuranceAggregateTask : KreateTask(
    "Puts the gates of all projects of the build into one report.",
    KreateTaskGroup.ASSURANCE
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val summaries: ConfigurableFileCollection

    @get:Input
    public abstract val members: SetProperty<String>

    @get:Input
    public abstract val formats: SetProperty<ReportFormat>

    @get:OutputDirectory
    public abstract val outputDirectory: DirectoryProperty

    @TaskAction
    public fun aggregate() {
        val files = summaries.filter { file -> file.isFile }
        val projects = files.mapNotNull { file -> ProjectSummaryJson.parse(file.readText(), file.name) }
        val sorted = projects.distinctBy { summary -> summary.project }.sortedBy { summary -> summary.project }
        val unchecked = (members.get() - sorted.map { summary -> summary.project }.toSet()).sorted()
        val report = AggregateReport(sorted, unchecked, Instant.now())
        val directory = outputDirectory.get().asFile
        directory.mkdirs()
        formats.get().forEach { format -> write(report, format, directory) }
        logger.lifecycle(AggregateWriters.render(report, ReportFormat.TEXT).orEmpty())
        fail(report)
    }

    private fun write(report: AggregateReport, format: ReportFormat, directory: File) {
        val text = AggregateWriters.render(report, format) ?: return
        directory.resolve("aggregate.${AGGREGATE_FORMATS.getValue(format)}").writeText(text)
    }

    private fun fail(report: AggregateReport) {
        val failure = failureOf(report) ?: return logger.lifecycle("Kreate assurance [PASSED] in every project.")
        throw failure
    }

    private fun failureOf(report: AggregateReport): GradleException? {
        val message = "${report.blockingFindings} blocking finding(s) in ${report.projects.size} project(s)."
        return when (report.status) {
            GateStatus.PASSED -> null
            GateStatus.FINDINGS -> AssuranceFindingsFailure(message)
            GateStatus.INCOMPLETE -> AssuranceIncompleteFailure("A project could not finish its checks. $message")
            GateStatus.INVALID -> AssuranceIncompleteFailure("A project has an invalid input. $message")
        }
    }
}
