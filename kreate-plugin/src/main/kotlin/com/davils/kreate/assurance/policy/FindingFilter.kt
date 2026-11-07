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
package com.davils.kreate.assurance.policy

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.isAtLeast
import com.davils.kreate.assurance.finding.withAttribute
import java.time.LocalDate

private const val IGNORE_FORBIDDEN_ATTRIBUTE: String = "ignoreForbidden"

internal class FindingFilter(
    private val minSeverity: Severity,
    private val rules: List<IgnoreRule>,
    private val today: LocalDate,
    private val baseline: BaselineMatcher,
    private val unignorable: Set<Severity> = emptySet()
) {
    fun apply(findings: List<Finding>, completeSources: Set<FindingSource>): FilterOutcome {
        val (active, expired) = rules.partition { rule -> !rule.isExpired(today) }
        val relevant = findings.filter { finding -> isRelevant(finding) }
        val (protected, ignorable) = relevant.partition { finding -> isProtected(finding) }
        val (suppressed, candidates) = ignorable.partition { finding -> isIgnored(finding, active) }
        val match = baseline.partition(candidates, completeSources)

        return FilterOutcome(
            kept = match.kept + protected.map { finding -> markedIfIgnored(finding, active) },
            suppressed = suppressed,
            baselined = match.baselined,
            expiredRules = expired,
            staleEntries = match.staleEntries
        )
    }

    private fun isProtected(finding: Finding): Boolean =
        finding.source == FindingSource.POLICY || finding.gatingSeverity in unignorable

    private fun isRelevant(finding: Finding): Boolean {
        if (finding.source == FindingSource.POLICY) return true
        val isUnratedAdvisory = finding.severity == Severity.UNKNOWN && finding.gatingSeverity != Severity.UNKNOWN
        return isUnratedAdvisory || finding.gatingSeverity.isAtLeast(minSeverity)
    }

    private fun isIgnored(finding: Finding, active: List<IgnoreRule>): Boolean =
        active.any { rule -> rule.matches(finding) }

    private fun markedIfIgnored(finding: Finding, active: List<IgnoreRule>): Finding {
        if (!isIgnored(finding, active)) return finding
        return finding.withAttribute(IGNORE_FORBIDDEN_ATTRIBUTE, true.toString())
    }
}
