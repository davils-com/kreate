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

private const val XML_DECLARATION: String = """<?xml version="1.0" encoding="UTF-8"?>"""

internal object CheckstyleReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val byFile = evaluation.outcome.kept.groupBy(ReportFacts::filePathOf).toSortedMap()
        val files = byFile.map { (path, findings) -> fileOf(path, findings) }
        val lines = listOf(XML_DECLARATION, """<checkstyle version="4.3">""") + files + "</checkstyle>"
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun fileOf(path: String, findings: List<Finding>): String {
        val errors = ReportFacts.ordered(findings).map(::errorOf)
        val header = """  <file name="${ReportEscaping.xml(path)}">"""
        return (listOf(header) + errors + "  </file>").joinToString("\n")
    }

    private fun errorOf(finding: Finding): String {
        val message = ReportEscaping.xml(finding.title)
        val source = ReportEscaping.xml("kreate.${finding.source.id}.${finding.ruleId}")
        val severity = severityOf(finding.gatingSeverity)
        val line = ReportFacts.lineOf(finding)
        return """    <error line="$line" severity="$severity" message="$message" source="$source"/>"""
    }

    private fun severityOf(severity: Severity): String = when (severity) {
        Severity.CRITICAL, Severity.HIGH -> "error"
        Severity.MEDIUM -> "warning"
        Severity.LOW, Severity.UNKNOWN -> "info"
    }
}
