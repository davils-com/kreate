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
package com.davils.kreate.governance

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.governance.outdated.OutdatedAssessor
import com.davils.kreate.governance.outdated.OutdatedModule
import com.davils.kreate.governance.outdated.OutdatedSettings
import com.davils.kreate.provenance.boundedMap
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.provenance.depsdev.DepsDevUnavailableException
import com.davils.kreate.provenance.isInternal
import java.time.Clock

private const val PARALLEL_REQUESTS: Int = 8

internal class OutdatedAuditor(
    private val client: () -> DepsDevClient,
    private val internalGroups: Set<String>,
    private val settings: OutdatedSettings,
    private val clock: Clock
) {
    fun audit(direct: List<ModuleCoordinate>, offline: Boolean): Pair<FindingDocument, List<OutdatedModule>> {
        if (offline) return incomplete("deps.dev cannot be read in offline mode.") to emptyList()
        val modules = direct.distinct().filterNot { module -> isInternal(module, internalGroups) }
        return try {
            val depsDev = client()
            val histories = boundedMap(modules, PARALLEL_REQUESTS) { module -> depsDev.historyOf(module) }
            val known = modules.zip(histories).mapNotNull { (module, history) -> history?.let { module to it } }
            val result = OutdatedAssessor(settings).assess(known)
            val now = clock.instant()
            val status = DocumentStatus.COMPLETE
            val document = FindingDocument(FindingSource.DEPENDENCIES, status, now, result.findings, dataAsOf = now)
            document to result.modules
        } catch (cause: DepsDevUnavailableException) {
            incomplete(cause.message.orEmpty()) to emptyList()
        }
    }

    private fun incomplete(reason: String): FindingDocument =
        FindingDocument(FindingSource.DEPENDENCIES, DocumentStatus.INCOMPLETE, clock.instant(), reason = reason)
}
