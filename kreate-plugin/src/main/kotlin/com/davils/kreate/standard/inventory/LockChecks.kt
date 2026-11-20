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
package com.davils.kreate.standard.inventory

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.standard.lock.LockEntry
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardLinks
import com.davils.kreate.standard.rule.StandardFindings
import com.davils.kreate.standard.rule.StandardRules
import java.time.Duration
import java.time.LocalDate

internal class LockChecks(
    private val declarations: List<StandardDeclaration>,
    private val defaultReviewEvery: Duration?,
    private val lockPath: String
) {
    fun findingsOf(lock: StandardsLock, today: LocalDate): List<Finding> {
        val declared = declarations.map { declaration -> declaration.id }.toSet()
        val reviews = declarations.mapNotNull { declaration -> reviewFinding(declaration, lock, today) }
        val orphaned = lock.entries.filterNot { entry -> entry.id in declared }
        return reviews + orphaned.map { entry -> orphanedFinding(entry) }
    }

    private fun reviewFinding(declaration: StandardDeclaration, lock: StandardsLock, today: LocalDate): Finding? {
        val entry = lock.entryOf(declaration.id) ?: return unreviewedFinding(declaration)
        val interval = declaration.reviewEverySeconds?.let(Duration::ofSeconds) ?: defaultReviewEvery ?: return null
        val due = entry.reviewedOn.plusDays(interval.toDays())
        if (!today.isAfter(due)) return null
        return StandardFindings.findingOf(
            ruleId = StandardRules.OVERDUE_RULE,
            id = declaration.id,
            title = "The review of ${declaration.id} from ${entry.reviewedOn} was due on $due",
            url = StandardLinks.infoUrlOf(declaration),
            attributes = mapOf("reviewedOn" to entry.reviewedOn.toString(), "due" to due.toString()),
            location = SourceLocation(lockPath)
        )
    }

    private fun unreviewedFinding(declaration: StandardDeclaration): Finding = StandardFindings.findingOf(
        ruleId = StandardRules.UNREVIEWED_RULE,
        id = declaration.id,
        title = "${declaration.id} is declared but was never reviewed",
        url = StandardLinks.infoUrlOf(declaration),
        location = SourceLocation(lockPath)
    )

    private fun orphanedFinding(entry: LockEntry): Finding = StandardFindings.findingOf(
        ruleId = StandardRules.ORPHANED_RULE,
        id = entry.id,
        title = "$lockPath records ${entry.id}, which is no longer declared",
        location = SourceLocation(lockPath)
    )
}
