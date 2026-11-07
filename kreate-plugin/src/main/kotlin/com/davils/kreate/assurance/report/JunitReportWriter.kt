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

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument

private const val XML_DECLARATION: String = """<?xml version="1.0" encoding="UTF-8"?>"""

internal object JunitReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val kept = evaluation.outcome.kept
        val suites = evaluation.documents.map { document -> suiteOf(document, kept) }
        val header = """<testsuites name="kreate assurance" tests="${testsOf(evaluation)}" failures="${kept.size}">"""
        val lines = listOf(XML_DECLARATION, header) + suites + "</testsuites>"
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun testsOf(evaluation: AssuranceEvaluation): Int {
        val kept = evaluation.outcome.kept
        val clean = evaluation.documents.count { document -> isClean(document, kept) }
        return kept.size + clean
    }

    private fun isClean(document: FindingDocument, kept: List<Finding>): Boolean =
        kept.none { finding -> finding.source == document.source }

    private fun suiteOf(document: FindingDocument, kept: List<Finding>): String {
        val name = ReportEscaping.xml(document.source.id)
        val findings = kept.filter { finding -> finding.source == document.source }
        val cases = findings.map(::caseOf).ifEmpty { listOf(cleanCaseOf(document)) }
        val errors = errorsOf(document)
        val header = """  <testsuite name="$name" tests="${cases.size}" failures="${findings.size}" errors="$errors">"""
        return (listOf(header) + cases + "  </testsuite>").joinToString("\n")
    }

    private fun errorsOf(document: FindingDocument): Int {
        if (document.status == DocumentStatus.INCOMPLETE) return 1
        return 0
    }

    private fun caseOf(finding: Finding): String {
        val name = ReportEscaping.xml("${finding.ruleId} ${ReportFacts.placeOf(finding)}")
        val type = ReportEscaping.xml(finding.gatingSeverity.name)
        val message = ReportEscaping.xml(finding.title)
        val details = ReportEscaping.xml(finding.description)
        val source = ReportEscaping.xml(finding.source.id)
        return """    <testcase classname="$source" name="$name">""" +
            """<failure type="$type" message="$message">$details</failure></testcase>"""
    }

    private fun cleanCaseOf(document: FindingDocument): String {
        val source = ReportEscaping.xml(document.source.id)
        if (document.status != DocumentStatus.INCOMPLETE) {
            return """    <testcase classname="$source" name="no findings"/>"""
        }
        val reason = ReportEscaping.xml(document.reason.orEmpty())
        return """    <testcase classname="$source" name="complete"><error message="$reason"/></testcase>"""
    }
}
