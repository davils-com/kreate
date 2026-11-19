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

import com.davils.kore.core.text.SafeText
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardLinks
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.rule.StandardFindings
import com.davils.kreate.standard.rule.StandardRules

private const val MAX_SHOWN_LENGTH: Int = 80

private const val MAX_LISTED: Int = 10

private const val REVIEWED: String = "reviewed"

private const val CURRENT: String = "current"

private const val NONE: String = "none"

internal class DriftDetector(
    private val declaration: StandardDeclaration,
    private val reviewed: StandardSnapshot,
    private val current: StandardSnapshot
) {
    private val id = declaration.id

    fun findings(): List<Finding> = listOfNotNull(
        obsoleted(),
        updated(),
        revised(),
        statusChanged(),
        errata(confirmed = true),
        errata(confirmed = false),
        changed()
    )

    private fun obsoleted(): Finding? {
        val replacements = current.replacedBy - reviewed.replacedBy.toSet()
        val isWithdrawn = current.withdrawn && !reviewed.withdrawn
        if (replacements.isEmpty() && !isWithdrawn) return null
        val replacedBy = listed(current.replacedBy)
        val title = "$id is obsoleted by $replacedBy".takeIf { current.replacedBy.isNotEmpty() } ?: "$id was withdrawn"
        return findingOf(StandardRules.OBSOLETED_RULE, title, mapOf("replacedBy" to replacedBy))
    }

    private fun updated(): Finding? {
        val updates = current.updatedBy - reviewed.updatedBy.toSet()
        if (updates.isEmpty()) return null
        val listed = listed(updates)
        return findingOf(StandardRules.UPDATED_RULE, "$id is updated by $listed", mapOf("updatedBy" to listed))
    }

    private fun revised(): Finding? {
        val revision = current.revision ?: return null
        if (revision == reviewed.revision) return null
        val before = shown(reviewed.revision ?: NONE)
        val after = shown(revision)
        val title = "$id has a new revision: $after, reviewed was $before"
        return findingOf(StandardRules.REVISED_RULE, title, mapOf(REVIEWED to before, CURRENT to after))
    }

    private fun statusChanged(): Finding? {
        val status = current.status ?: return null
        if (status == reviewed.status) return null
        val before = shown(reviewed.status ?: NONE)
        val after = shown(status)
        val title = "$id changed its status from $before to $after"
        return findingOf(StandardRules.STATUS_RULE, title, mapOf(REVIEWED to before, CURRENT to after))
    }

    private fun errata(confirmed: Boolean): Finding? {
        val known = reviewed.errata.filterValues { state -> state.isConfirmed == confirmed }.keys
        val fresh = current.errata.filter { (erratum, state) -> state.isConfirmed == confirmed && erratum !in known }
        if (fresh.isEmpty()) return null
        val ordered = fresh.keys.sortedWith(compareBy({ erratum -> erratum.length }, { erratum -> erratum }))
        val listed = listed(ordered)
        val title = "$id has ${fresh.size} new ${kindOf(confirmed)} errata: $listed"
        val attributes = mapOf("errata" to listed, "states" to statesOf(fresh))
        return StandardFindings.findingOf(ruleOf(confirmed), id, title, StandardLinks.erratumUrlOf(id), attributes)
    }

    private fun changed(): Finding? {
        val fingerprint = current.fingerprint ?: return null
        val before = reviewed.fingerprint ?: return null
        if (fingerprint == before) return null
        val title = "The watched page of $id changed since its review"
        return findingOf(StandardRules.CHANGED_RULE, title, mapOf(REVIEWED to before, CURRENT to fingerprint))
    }

    private fun findingOf(rule: String, title: String, attributes: Map<String, String>): Finding {
        val url = StandardLinks.infoUrlOf(id, declaration.url)
        return StandardFindings.findingOf(rule, id, title, url, attributes)
    }

    private fun kindOf(confirmed: Boolean): String = when (confirmed) {
        true -> "confirmed"
        false -> "reported"
    }

    private fun ruleOf(confirmed: Boolean): String = when (confirmed) {
        true -> StandardRules.ERRATA_RULE
        false -> StandardRules.REPORTED_ERRATA_RULE
    }

    private fun listed(texts: Collection<String>): String {
        val shownTexts = texts.take(MAX_LISTED).map { text -> shown(text) }
        val more = texts.size - shownTexts.size
        val suffix = " and $more more".takeIf { more > 0 }.orEmpty()
        return shownTexts.joinToString() + suffix
    }

    private fun statesOf(errata: Map<String, ErratumState>): String {
        val states = errata.values.map { state -> state.name }
        val distinct = states.distinct()
        return distinct.sorted().joinToString()
    }

    private fun shown(text: String): String = SafeText.text(text, MAX_SHOWN_LENGTH)
}
