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
package com.davils.kreate.standard.lock

import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.closestMatch
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIdGrammar
import com.davils.kreate.standard.model.StandardSnapshot
import java.time.LocalDate

internal class ReviewPlan(
    private val declarations: List<StandardDeclaration>,
    private val lock: StandardsLock,
    private val snapshots: Map<StandardId, StandardSnapshot>
) {
    private val declared: List<StandardId> = declarations.map { declaration -> declaration.id }

    val pending: List<StandardId>
        get() = declared.filter { id -> lock.entryOf(id)?.snapshot != snapshots[id] }

    val orphaned: Set<StandardId>
        get() = lock.ids - declared.toSet()

    fun chosen(names: List<String>, all: Boolean): List<StandardId> {
        if (all) return declared
        return names.map { name -> idOf(name) }.distinct()
    }

    fun reviewed(ids: List<StandardId>, note: String, today: LocalDate): StandardsLock {
        val unavailable = ids.filterNot { id -> id in snapshots }
        if (unavailable.isNotEmpty()) {
            val listed = unavailable.joinToString()
            throw ReviewException("Cannot review $listed: the audit could not read what its body publishes today.")
        }
        val reviews = ids.map { id -> LockEntry(id, today, snapshots.getValue(id), note) }
        return lock.withReviews(reviews)
    }

    private fun idOf(name: String): StandardId {
        val trimmed = name.trim()
        val id = StandardIdGrammar.parse(trimmed)
        if (id != null && id in declared) return id
        val known = declared.map { candidate -> candidate.toString() }
        val match = closestMatch(trimmed, known, ignoreCase = true)
        val hint = match?.let { candidate -> " Did you mean $candidate?" }.orEmpty()
        throw ReviewException("${SafeText.quoted(trimmed)} is no declared standard.$hint")
    }
}
