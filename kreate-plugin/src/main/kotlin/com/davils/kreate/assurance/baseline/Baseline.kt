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

internal object Baseline {
    fun record(findings: List<Finding>, maxSeverity: Severity? = null): List<BaselineEntry> {
        val recordable = findings.filter { finding -> isRecordable(finding, maxSeverity) }
        val groups = recordable.groupBy { finding -> BaselineEntry.keyOf(finding) }
        val entries = groups.values.map { group -> entryOf(group) }
        return entries.sortedBy { entry -> entry.key }
    }

    fun prune(
        entries: List<BaselineEntry>,
        findings: List<Finding>,
        completeSources: Set<FindingSource>
    ): List<BaselineEntry> {
        val occurrences = findings.groupingBy { finding -> BaselineEntry.keyOf(finding) }.eachCount()
        val completeIds = completeSources.map { source -> source.id }
        return entries.mapNotNull { entry -> pruned(entry, occurrences, completeIds) }
    }

    private fun isRecordable(finding: Finding, maxSeverity: Severity?): Boolean {
        val limit = maxSeverity ?: return true
        return limit.isAtLeast(finding.gatingSeverity)
    }

    private fun entryOf(group: List<Finding>): BaselineEntry {
        val first = group.first()
        val mostSevere = group.minOf { finding -> finding.gatingSeverity }
        return BaselineEntry(
            source = first.source.id,
            rule = first.ruleId,
            count = group.size,
            severity = mostSevere,
            title = first.title,
            module = first.module,
            path = first.location?.path
        )
    }

    private fun pruned(
        entry: BaselineEntry,
        occurrences: Map<String, Int>,
        completeIds: List<String>
    ): BaselineEntry? {
        if (entry.source !in completeIds) return entry

        val occurring = occurrences[entry.key] ?: 0
        if (occurring == 0) return null
        return entry.copy(count = minOf(entry.count, occurring))
    }
}
