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
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.isAtLeast
import java.time.Duration
import java.time.Instant

internal const val REPORT_ONLY_ATTRIBUTE: String = "reportOnly"

internal class VerdictRules(
    private val failOn: Severity,
    private val reportOnly: Boolean = false,
    private val failOnStale: Boolean = false,
    private val maxDataAge: Duration? = null
) {
    fun decide(documents: List<FindingDocument>, outcome: FilterOutcome, now: Instant = Instant.EPOCH): Verdict {
        val blocking = outcome.kept.count { finding -> isBlocking(finding) }
        val incomplete = documents.filter { document -> isIncomplete(document, now) }
        val incompleteSources = incomplete.map { document -> document.source.id }
        val status = statusOf(incompleteSources, blocking, outcome.staleEntries)
        return Verdict(status, blocking, incompleteSources, outcome.staleEntries)
    }

    private fun isIncomplete(document: FindingDocument, now: Instant): Boolean {
        if (document.status == DocumentStatus.INCOMPLETE) return true

        val dataAsOf = document.dataAsOf ?: return false
        val limit = maxDataAge ?: return false
        return dataAsOf.plus(limit).isBefore(now)
    }

    private fun isBlocking(finding: Finding): Boolean {
        val isOnlyReported = REPORT_ONLY_ATTRIBUTE in finding.attributes
        return !isOnlyReported && finding.gatingSeverity.isAtLeast(failOn)
    }

    private fun statusOf(incompleteSources: List<String>, blocking: Int, staleEntries: Int): GateStatus {
        if (incompleteSources.isNotEmpty()) return GateStatus.INCOMPLETE

        val hasStaleEntries = failOnStale && staleEntries > 0
        val hasFailures = blocking > 0 || hasStaleEntries
        if (hasFailures && !reportOnly) return GateStatus.FINDINGS
        return GateStatus.PASSED
    }
}
