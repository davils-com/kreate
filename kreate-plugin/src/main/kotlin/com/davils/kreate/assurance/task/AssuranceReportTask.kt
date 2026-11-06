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
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.report.AssuranceEvaluation
import com.davils.kreate.assurance.report.JsonReportWriter
import com.davils.kreate.assurance.report.SarifReportWriter
import com.davils.kreate.assurance.report.TextReportWriter
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The report depends on the day it is written")
internal abstract class AssuranceReportTask : AssuranceEvaluationTask("Writes the assurance reports.") {
    @get:Input
    public abstract val formats: SetProperty<ReportFormat>

    @get:OutputDirectory
    public abstract val outputDirectory: DirectoryProperty

    @TaskAction
    public fun write() {
        val evaluation = try {
            evaluate()
        } catch (cause: AssuranceInputException) {
            throw AssuranceInvalidFailure(cause.message.orEmpty(), cause)
        }

        formats.get().forEach { format -> writeReport(format, evaluation) }
    }

    private fun writeReport(format: ReportFormat, evaluation: AssuranceEvaluation) {
        val directory = outputDirectory.get().asFile
        val fileName = "report.${extensionOf(format)}"
        directory.resolve(fileName).writeText(render(format, evaluation))
    }

    private fun extensionOf(format: ReportFormat): String = when (format) {
        ReportFormat.JSON -> "json"
        ReportFormat.SARIF -> "sarif"
        ReportFormat.TEXT -> "txt"
    }

    private fun render(format: ReportFormat, evaluation: AssuranceEvaluation): String = when (format) {
        ReportFormat.JSON -> JsonReportWriter.render(evaluation)
        ReportFormat.SARIF -> SarifReportWriter.render(evaluation)
        ReportFormat.TEXT -> TextReportWriter.render(evaluation)
    }
}
