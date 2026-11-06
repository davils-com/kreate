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
package com.davils.kreate.assurance.baseline

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.isAtLeast

internal class BaselineMatcher(
    private val entries: List<BaselineEntry>,
    private val maxSeverity: Severity? = null
) {
    fun partition(findings: List<Finding>, completeSources: Set<FindingSource>): BaselineMatch {
        val groups = findings.groupBy { finding -> BaselineEntry.keyOf(finding) }
        val budget = entries.associate { entry -> entry.key to entry.count }
        val covered = groups.flatMap { (key, group) -> coveredBy(budget[key] ?: 0, group) }
        val coveredSet = identitySet(covered)
        val (baselined, kept) = findings.partition { finding -> finding in coveredSet }

        return BaselineMatch(kept, baselined, staleEntries(groups, completeSources))
    }

    private fun coveredBy(allowed: Int, group: List<Finding>): List<Finding> {
        val eligible = group.filter { finding -> isCoverable(finding) }
        val inLineOrder = eligible.sortedBy { finding -> finding.location?.line ?: 0 }
        return inLineOrder.take(allowed)
    }

    private fun isCoverable(finding: Finding): Boolean {
        val limit = maxSeverity ?: return true
        return limit.isAtLeast(finding.gatingSeverity)
    }

    private fun staleEntries(groups: Map<String, List<Finding>>, completeSources: Set<FindingSource>): Int {
        val completeIds = completeSources.map { source -> source.id }
        val checked = entries.filter { entry -> entry.source in completeIds }
        return checked.sumOf { entry -> missingOccurrences(entry, groups) }
    }

    private fun missingOccurrences(entry: BaselineEntry, groups: Map<String, List<Finding>>): Int {
        val occurring = groups[entry.key]?.size ?: 0
        return (entry.count - occurring).coerceAtLeast(0)
    }

    private fun identitySet(findings: List<Finding>): MutableSet<Finding> {
        val set = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Finding, Boolean>())
        set.addAll(findings)
        return set
    }
}
