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
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.isAtLeast

internal class VerdictRules(
    private val failOn: Severity,
    private val reportOnly: Boolean = false,
    private val failOnStale: Boolean = false
) {
    fun decide(documents: List<FindingDocument>, outcome: FilterOutcome): Verdict {
        val blocking = outcome.kept.count { finding -> finding.gatingSeverity.isAtLeast(failOn) }
        val incomplete = documents.filter { document -> document.status == DocumentStatus.INCOMPLETE }
        val incompleteSources = incomplete.map { document -> document.source.id }
        val status = statusOf(incompleteSources, blocking, outcome.staleEntries)
        return Verdict(status, blocking, incompleteSources, outcome.staleEntries)
    }

    private fun statusOf(incompleteSources: List<String>, blocking: Int, staleEntries: Int): GateStatus {
        if (incompleteSources.isNotEmpty()) return GateStatus.INCOMPLETE

        val hasStaleEntries = failOnStale && staleEntries > 0
        val hasFailures = blocking > 0 || hasStaleEntries
        if (hasFailures && !reportOnly) return GateStatus.FINDINGS
        return GateStatus.PASSED
    }
}
