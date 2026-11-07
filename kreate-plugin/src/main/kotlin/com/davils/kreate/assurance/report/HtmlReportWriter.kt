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
package com.davils.kreate.assurance.report

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument

internal object HtmlReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val project = ReportEscaping.xml(evaluation.project)
        val status = evaluation.verdict.status.name
        val version = ReportEscaping.xml(evaluation.toolVersion)
        val body = listOf(
            """<h1>Kreate assurance <span class="status $status">$status</span></h1>""",
            """<p class="meta">$project · ${evaluation.generatedAt} · Kreate $version</p>""",
            cardsOf(evaluation),
            sourcesOf(evaluation.documents),
            findingsOf(evaluation.outcome.kept)
        ).joinToString("\n")
        return HtmlDashboard.page("Kreate assurance: ${evaluation.project}", body)
    }

    fun cardsOf(evaluation: AssuranceEvaluation): String {
        val counts = ReportFacts.countsOf(evaluation.outcome.kept)
        val severities = counts.map { (severity, count) -> card(count.toString(), severity.name, severity.name) }
        val extra = listOf(
            card(evaluation.verdict.blockingFindings.toString(), "blocking", ""),
            card(evaluation.outcome.suppressed.size.toString(), "suppressed", ""),
            card(evaluation.outcome.baselined.size.toString(), "baselined", "")
        )
        return """<div class="cards">""" + (extra + severities).joinToString("") + "</div>"
    }

    fun findingsOf(findings: List<Finding>): String {
        if (findings.isEmpty()) return "<h2>Findings</h2><p>No findings.</p>"
        val filters = Severity.entries.joinToString("") { severity ->
            """<label><input type="checkbox" data-severity="${severity.name}" checked>${severity.name}</label>"""
        }
        val rows = ReportFacts.ordered(findings).joinToString("\n", transform = ::rowOf)
        val search = """<input id="search" type="search" placeholder="Filter findings" aria-label="Filter findings">"""
        val head = "<thead><tr><th>Severity</th><th>Rule</th><th>Where</th><th>Finding</th></tr></thead>"
        return """<h2>Findings</h2><div class="filters">$search$filters</div>""" +
            "<table>$head<tbody>\n$rows\n</tbody></table>"
    }

    private fun sourcesOf(documents: List<FindingDocument>): String {
        val rows = documents.joinToString("") { document ->
            val reason = ReportEscaping.xml(document.reason.orEmpty())
            val source = ReportEscaping.xml(document.source.id)
            "<tr><td>$source</td><td>${document.status.name}</td><td>$reason</td></tr>"
        }
        val head = "<thead><tr><th>Source</th><th>Status</th><th>Note</th></tr></thead>"
        return "<h2>Sources</h2><table>$head<tbody>$rows</tbody></table>"
    }

    private fun rowOf(finding: Finding): String {
        val severity = finding.gatingSeverity.name
        val where = ReportEscaping.xml(ReportFacts.placeOf(finding))
        val rule = ReportEscaping.xml("${finding.source.id}/${finding.ruleId}")
        val title = ReportEscaping.xml(finding.title)
        val description = ReportEscaping.xml(finding.description)
        return """<tr data-severity="$severity"><td class="$severity">$severity</td><td>$rule</td>""" +
            """<td class="where">$where</td><td><b>$title</b><br>$description</td></tr>"""
    }

    private fun card(value: String, label: String, style: String): String =
        """<div class="card"><b class="$style">$value</b>$label</div>"""
}
