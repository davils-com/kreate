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

internal object TextReportWriter {
    fun render(evaluation: AssuranceEvaluation): String = buildString {
        val verdict = evaluation.verdict
        val outcome = evaluation.outcome

        appendLine("Kreate assurance report for ${evaluation.project}")
        appendLine("Status: ${verdict.status.name}, ${verdict.blockingFindings} blocking finding(s)")
        appendLine("Suppressed: ${outcome.suppressed.size}, baselined: ${outcome.baselined.size}")
        outcome.expiredRules.forEach { rule -> appendLine("Expired ignore rule: ${rule.id}") }
        verdict.incompleteSources.forEach { source -> appendLine("Incomplete: $source") }

        val bySeverity = outcome.kept.sortedBy { finding -> finding.gatingSeverity }
        bySeverity.forEach { finding -> appendLine(lineOf(finding)) }
    }

    private fun lineOf(finding: Finding): String {
        val place = finding.location?.path ?: finding.module.orEmpty()
        return "  ${finding.gatingSeverity.name} ${finding.ruleId} $place ${finding.title}"
    }
}
