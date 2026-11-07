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

import com.davils.kreate.assurance.finding.Finding

private const val MAX_ROWS: Int = 200

internal object MarkdownReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val verdict = evaluation.verdict
        val kept = ReportFacts.ordered(evaluation.outcome.kept)
        val counts = ReportFacts.countsOf(kept).filterValues { count -> count > 0 }
        val lines = mutableListOf(
            "## Kreate assurance: ${verdict.status.name}",
            "",
            "${ReportEscaping.markdown(evaluation.project)}: ${verdict.blockingFindings} blocking finding(s), " +
                "${evaluation.outcome.suppressed.size} suppressed, ${evaluation.outcome.baselined.size} baselined.",
            ""
        )
        val summary = counts.entries.joinToString(" · ") { (severity, count) -> "$count ${severity.name}" }
        if (counts.isNotEmpty()) lines += summary
        verdict.incompleteSources.forEach { source -> lines += "- Incomplete: ${ReportEscaping.markdown(source)}" }
        if (kept.isEmpty()) return (lines + "" + "No findings.").joinToString("\n", postfix = "\n")
        lines += listOf("", "| Severity | Rule | Where | Finding |", "|---|---|---|---|")
        lines += kept.take(MAX_ROWS).map(::rowOf)
        if (kept.size > MAX_ROWS) lines += listOf("", "${kept.size - MAX_ROWS} more finding(s) are in the JSON report.")
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun rowOf(finding: Finding): String {
        val cells = listOf(finding.gatingSeverity.name, finding.ruleId, ReportFacts.placeOf(finding), finding.title)
        return cells.joinToString(" | ", prefix = "| ", postfix = " |") { cell -> ReportEscaping.markdown(cell) }
    }
}
