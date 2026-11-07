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
package com.davils.kreate.provenance

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.provenance.depsdev.DepsDevClient
import com.davils.kreate.provenance.depsdev.DepsDevUnavailableException
import com.davils.kreate.provenance.trust.TrustAssessor
import com.davils.kreate.provenance.trust.TrustEvidence
import com.davils.kreate.provenance.trust.TrustThresholds
import java.time.Clock

private const val PARALLEL_REQUESTS: Int = 8

internal class TrustAuditor(
    private val client: () -> DepsDevClient,
    private val internalGroups: Set<String>,
    private val thresholds: TrustThresholds,
    private val clock: Clock
) {
    fun audit(graphs: List<ResolvedGraph>, offline: Boolean): FindingDocument {
        if (offline) return incomplete("deps.dev cannot be read in offline mode.")

        val modules = graphs.flatMap { graph -> graph.modules }.distinct()
        val assessed = modules.filterNot { module -> isInternal(module, internalGroups) }
        val unresolved = graphs.flatMap { graph -> graph.unresolved }.distinct()

        return try {
            val evidence = evidenceOf(client(), assessed)
            val assessor = TrustAssessor(thresholds, clock.instant())
            documentOf(evidence.flatMap { entry -> assessor.assess(entry) }, unresolved)
        } catch (cause: DepsDevUnavailableException) {
            incomplete(cause.message.orEmpty())
        }
    }

    private fun evidenceOf(depsDev: DepsDevClient, modules: List<ModuleCoordinate>): List<TrustEvidence> {
        val histories = boundedMap(modules, PARALLEL_REQUESTS) { module ->
            TrustEvidence(module, depsDev.historyOf(module))
        }
        if (thresholds.minScorecard <= 0.0) return histories

        val known = histories.filter { entry -> entry.history != null }
        val withProjects = boundedMap(known, PARALLEL_REQUESTS) { entry ->
            entry.copy(project = projectOf(depsDev, entry))
        }
        val projects = withProjects.mapNotNull { entry -> entry.project }.distinct()
        val scores = boundedMap(projects, PARALLEL_REQUESTS) { project -> depsDev.scorecardOf(project) }
        val byProject = projects.zip(scores).toMap()
        val scored = withProjects.map { entry ->
            entry.copy(scorecard = byProject[entry.project])
        }
        return histories.filter { entry -> entry.history == null } + scored
    }

    private fun projectOf(depsDev: DepsDevClient, evidence: TrustEvidence): String? {
        val projects = depsDev.sourceProjectsOf(evidence.module)
        return projects.firstOrNull()
    }

    private fun documentOf(findings: List<Finding>, unresolved: List<String>): FindingDocument {
        val now = clock.instant()
        if (unresolved.isEmpty()) {
            return FindingDocument(FindingSource.TRUST, DocumentStatus.COMPLETE, now, findings, dataAsOf = now)
        }
        val reason = "These dependencies could not be resolved, so they were not assessed: $unresolved"
        return FindingDocument(FindingSource.TRUST, DocumentStatus.INCOMPLETE, now, findings, reason, now)
    }

    private fun incomplete(reason: String): FindingDocument =
        FindingDocument(FindingSource.TRUST, DocumentStatus.INCOMPLETE, clock.instant(), reason = reason)
}
