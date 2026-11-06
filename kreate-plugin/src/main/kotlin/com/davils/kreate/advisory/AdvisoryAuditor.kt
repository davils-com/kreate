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
package com.davils.kreate.advisory

import com.davils.kreate.advisory.osv.OsvClient
import com.davils.kreate.advisory.osv.OsvRecord
import com.davils.kreate.advisory.osv.OsvUnavailableException
import com.davils.kreate.advisory.osv.fixedVersionFor
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.report.SnippetSanitizer
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import java.time.Clock

private const val ADVISORY_URL: String = "https://osv.dev/vulnerability/"

internal class AdvisoryAuditor(
    private val client: () -> OsvClient,
    private val internalGroups: Set<String>,
    private val clock: Clock
) {
    fun audit(graphs: List<ResolvedGraph>, offline: Boolean): FindingDocument {
        val unresolved = graphs.flatMap { graph -> graph.unresolved }.distinct()
        if (offline) return incomplete("Advisories cannot be read in offline mode.")

        val modules = graphs.flatMap { graph -> graph.modules }.distinct()
        val auditable = modules.filterNot { module -> isInternal(module) }

        return try {
            val records = client().recordsOf(auditable)
            val findings = records.flatMap { (module, moduleRecords) -> findingsOf(module, moduleRecords) }
            documentOf(findings, unresolved)
        } catch (cause: OsvUnavailableException) {
            incomplete(cause.message.orEmpty())
        }
    }

    private fun isInternal(module: ModuleCoordinate): Boolean = internalGroups.any { group ->
        module.group == group || module.group.startsWith("$group.")
    }

    private fun documentOf(findings: List<Finding>, unresolved: List<String>): FindingDocument {
        val now = clock.instant()
        if (unresolved.isEmpty()) {
            return FindingDocument(FindingSource.OSV, DocumentStatus.COMPLETE, now, findings, dataAsOf = now)
        }

        val reason = "These dependencies could not be resolved, so they were not audited: $unresolved"
        return FindingDocument(FindingSource.OSV, DocumentStatus.INCOMPLETE, now, findings, reason, now)
    }

    private fun incomplete(reason: String): FindingDocument =
        FindingDocument(FindingSource.OSV, DocumentStatus.INCOMPLETE, clock.instant(), reason = reason)

    private fun findingsOf(module: ModuleCoordinate, records: List<OsvRecord>): List<Finding> {
        val active = records.filterNot { record -> record.isWithdrawn }
        return active.map { record -> findingOf(module, record) }
    }

    private fun findingOf(module: ModuleCoordinate, record: OsvRecord): Finding {
        val ranges = record.ranges[module.module].orEmpty()
        return Finding(
            ruleId = record.id,
            source = FindingSource.OSV,
            severity = record.severity,
            title = SnippetSanitizer.escape(record.summary ?: record.id),
            description = SnippetSanitizer.escape(record.details.orEmpty()),
            module = module.module,
            version = module.version,
            fixedVersion = fixedVersionFor(module.version, ranges),
            aliases = record.aliases,
            url = ADVISORY_URL + record.id
        )
    }
}
