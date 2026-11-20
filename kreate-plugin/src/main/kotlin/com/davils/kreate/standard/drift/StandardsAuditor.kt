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
package com.davils.kreate.standard.drift

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardLinks
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.StandardProvider
import com.davils.kreate.standard.rule.StandardFindings
import com.davils.kreate.standard.rule.StandardRules
import java.time.Clock
import java.time.Instant

private const val OFFLINE_REASON: String = "Standards cannot be checked in offline mode."

internal class StandardsAuditor(
    private val providers: () -> List<StandardProvider>,
    private val oldestAnswer: () -> Instant?,
    private val clock: Clock
) {
    fun audit(declarations: List<StandardDeclaration>, lock: StandardsLock, offline: Boolean): AuditResult {
        val now = clock.instant()
        if (declarations.isEmpty()) return AuditResult(documentOf(DocumentStatus.SKIPPED, now), emptyMap())
        val manual = declarations.filterNot { declaration -> isChecked(declaration) }
        val manualSnapshots = manual.associate { declaration -> declaration.id to manualSnapshotOf(declaration) }
        val checked = declarations.filter { declaration -> isChecked(declaration) }
        if (checked.isEmpty()) {
            return AuditResult(documentOf(DocumentStatus.COMPLETE, now, dataAsOf = now), manualSnapshots)
        }
        if (offline) {
            return AuditResult(documentOf(DocumentStatus.INCOMPLETE, now, reason = OFFLINE_REASON), manualSnapshots)
        }
        val available = providers()
        val outcomes = checked.associate { declaration -> declaration.id to lookup(declaration, available) }
        return resultOf(checked, outcomes, lock, manualSnapshots, now)
    }

    private fun resultOf(
        checked: List<StandardDeclaration>,
        outcomes: Map<StandardId, Outcome<StandardSnapshot, LookupFailure>>,
        lock: StandardsLock,
        manualSnapshots: Map<StandardId, StandardSnapshot>,
        now: Instant
    ): AuditResult {
        val findings = checked.flatMap { declaration ->
            findingsOf(declaration, outcomes.getValue(declaration.id), lock)
        }
        val snapshots = outcomes.mapNotNull { (id, outcome) -> snapshotOf(id, outcome) }
        val missing = outcomes.values.mapNotNull { outcome -> unavailableReason(outcome) }
        val allSnapshots = manualSnapshots + snapshots.toMap()
        if (missing.isNotEmpty()) {
            val reason = missing.joinToString(" ")
            return AuditResult(documentOf(DocumentStatus.INCOMPLETE, now, findings, reason), allSnapshots)
        }
        val dataAsOf = oldestAnswer() ?: now
        return AuditResult(documentOf(DocumentStatus.COMPLETE, now, findings, dataAsOf = dataAsOf), allSnapshots)
    }

    private fun snapshotOf(
        id: StandardId,
        outcome: Outcome<StandardSnapshot, LookupFailure>
    ): Pair<StandardId, StandardSnapshot>? {
        val success = outcome as? Outcome.Success ?: return null
        return id to success.value
    }

    private fun lookup(
        declaration: StandardDeclaration,
        available: List<StandardProvider>
    ): Outcome<StandardSnapshot, LookupFailure> {
        val provider = available.firstOrNull { candidate -> candidate.accepts(declaration) }
            ?: return Outcome.Failure(LookupFailure.Unavailable("No service checks ${declaration.id}."))
        return provider.snapshotOf(declaration)
    }

    private fun findingsOf(
        declaration: StandardDeclaration,
        outcome: Outcome<StandardSnapshot, LookupFailure>,
        lock: StandardsLock
    ): List<Finding> = when (outcome) {
        is Outcome.Success -> driftOf(declaration, outcome.value, lock)
        is Outcome.Failure -> listOfNotNull(failureFinding(declaration, outcome.failure))
    }

    private fun driftOf(
        declaration: StandardDeclaration,
        current: StandardSnapshot,
        lock: StandardsLock
    ): List<Finding> {
        val reviewed = lock.entryOf(declaration.id) ?: return emptyList()
        return DriftDetector(declaration, reviewed.snapshot, current).findings()
    }

    private fun failureFinding(declaration: StandardDeclaration, failure: LookupFailure): Finding? = when (failure) {
        is LookupFailure.Unknown -> failureFinding(
            declaration,
            StandardRules.UNKNOWN_RULE,
            "The issuing body does not know ${declaration.id}",
            failure
        )
        is LookupFailure.Mismatch -> failureFinding(
            declaration,
            StandardRules.SUITE_MISMATCH_RULE,
            "The vendored copy of ${declaration.id} does not match what it pins",
            failure
        )
        is LookupFailure.Unavailable -> null
    }

    private fun failureFinding(
        declaration: StandardDeclaration,
        ruleId: String,
        title: String,
        failure: LookupFailure
    ): Finding = StandardFindings.findingOf(
        ruleId = ruleId,
        id = declaration.id,
        title = title,
        url = StandardLinks.infoUrlOf(declaration),
        attributes = mapOf("reason" to failure.reason)
    )

    private fun unavailableReason(outcome: Outcome<StandardSnapshot, LookupFailure>): String? {
        val failure = (outcome as? Outcome.Failure)?.failure ?: return null
        if (failure !is LookupFailure.Unavailable) return null
        return failure.reason
    }

    private fun isChecked(declaration: StandardDeclaration): Boolean =
        declaration.id.kind.isMachineChecked || declaration.url != null

    private fun manualSnapshotOf(declaration: StandardDeclaration): StandardSnapshot =
        StandardSnapshot(title = declaration.id.toString())

    private fun documentOf(
        status: DocumentStatus,
        evaluatedAt: Instant,
        findings: List<Finding> = emptyList(),
        reason: String? = null,
        dataAsOf: Instant? = null
    ): FindingDocument = FindingDocument(FindingSource.STANDARDS, status, evaluatedAt, findings, reason, dataAsOf)
}
