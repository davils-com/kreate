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
package com.davils.kreate.assurance.aggregate

import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingJson
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.report.HtmlDashboard
import com.davils.kreate.assurance.report.HtmlReportWriter
import com.davils.kreate.assurance.report.ReportEscaping
import com.davils.kreate.assurance.report.ReportFacts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val PRETTY_JSON: Json = Json { prettyPrint = true }

private const val STATUS_WIDTH: Int = 10

private const val TABLE_HEAD: String =
    "<thead><tr><th>Project</th><th>Status</th><th>Blocking</th><th>Findings</th></tr></thead>"

internal val AGGREGATE_FORMATS: Map<ReportFormat, String> = mapOf(
    ReportFormat.JSON to "json",
    ReportFormat.TEXT to "txt",
    ReportFormat.MARKDOWN to "md",
    ReportFormat.HTML to "html"
)

internal object AggregateWriters {
    fun render(report: AggregateReport, format: ReportFormat): String? = when (format) {
        ReportFormat.JSON -> json(report)
        ReportFormat.TEXT -> text(report)
        ReportFormat.MARKDOWN -> markdown(report)
        ReportFormat.HTML -> html(report)
        ReportFormat.SARIF, ReportFormat.JUNIT, ReportFormat.CHECKSTYLE, ReportFormat.GITLAB,
        ReportFormat.SONARQUBE -> null
    }

    private fun json(report: AggregateReport): String {
        val root = buildJsonObject {
            put("schemaVersion", 1)
            put("generatedAt", report.generatedAt.toString())
            put("status", report.status.name)
            put("exitCode", report.status.exitCode)
            put("blockingFindings", report.blockingFindings)
            put("projects", JsonArray(report.projects.map(::projectOf)))
            put("unchecked", JsonArray(report.unchecked.map(::JsonPrimitive)))
        }
        return PRETTY_JSON.encodeToString(JsonElement.serializer(), root) + "\n"
    }

    private fun projectOf(summary: ProjectSummary): JsonObject = buildJsonObject {
        put("project", summary.project)
        put("status", summary.status.name)
        put("exitCode", summary.status.exitCode)
        put("blockingFindings", summary.blockingFindings)
        put("incompleteSources", JsonArray(summary.incompleteSources.map(::JsonPrimitive)))
        put("findings", JsonArray(summary.findings.map(FindingJson::write)))
    }

    private fun text(report: AggregateReport): String {
        val lines = mutableListOf("Kreate assurance over ${report.projects.size} project(s): ${report.status.name}")
        lines += report.projects.map { summary ->
            "  ${summary.status.name.padEnd(STATUS_WIDTH)} ${summary.project} (${summary.blockingFindings} blocking)"
        }
        lines += report.unchecked.map { path -> "  NOT CHECKED $path" }
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun markdown(report: AggregateReport): String {
        val rows = report.projects.map { summary ->
            val name = ReportEscaping.markdown(summary.project)
            "| $name | ${summary.status.name} | ${summary.blockingFindings} | ${summary.findings.size} |"
        }
        val unchecked = report.unchecked.map { path -> "- Not checked: ${ReportEscaping.markdown(path)}" }
        val title = "## Kreate assurance: ${report.status.name}"
        val header = listOf(title, "", "| Project | Status | Blocking | Findings |")
        val lines = header + "|---|---|---|---|" + rows + "" + unchecked
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun html(report: AggregateReport): String {
        val status = report.status.name
        val rows = report.projects.joinToString("", transform = ::rowOf)
        val findings = report.projects.flatMap { summary ->
            summary.findings.map { finding -> located(summary.project, finding) }
        }
        val body = listOf(
            """<h1>Kreate assurance <span class="status $status">$status</span></h1>""",
            """<p class="meta">${report.projects.size} project(s) · ${report.generatedAt}</p>""",
            "<h2>Projects</h2><table>$TABLE_HEAD<tbody>$rows</tbody></table>",
            uncheckedOf(report.unchecked),
            HtmlReportWriter.findingsOf(findings)
        ).joinToString("\n")
        return HtmlDashboard.page("Kreate assurance", body)
    }

    private fun rowOf(summary: ProjectSummary): String {
        val name = ReportEscaping.xml(summary.project)
        val status = summary.status.name
        return """<tr><td>$name</td><td class="$status">$status</td>""" +
            "<td>${summary.blockingFindings}</td><td>${summary.findings.size}</td></tr>"
    }

    private fun uncheckedOf(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        val items = paths.joinToString("") { path -> "<li>${ReportEscaping.xml(path)}</li>" }
        return "<h2>Not checked</h2><ul>$items</ul>"
    }

    private fun located(project: String, finding: Finding): Finding {
        val place = "$project: ${ReportFacts.placeOf(finding)}"
        return finding.copy(location = SourceLocation(place, finding.location?.line))
    }
}
