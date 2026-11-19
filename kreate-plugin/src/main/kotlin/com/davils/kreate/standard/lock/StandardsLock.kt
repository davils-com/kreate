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

import com.davils.kreate.standard.model.StandardId

internal class StandardsLock(val entries: List<LockEntry> = emptyList()) {
    val ids: Set<StandardId>
        get() = entries.map { entry -> entry.id }.toSet()

    fun entryOf(id: StandardId): LockEntry? = entries.firstOrNull { entry -> entry.id == id }

    fun withReviews(reviews: List<LockEntry>): StandardsLock {
        val reviewed = reviews.map { review -> review.id }.toSet()
        val kept = entries.filterNot { entry -> entry.id in reviewed }
        val merged = kept + reviews
        return StandardsLock(merged.sortedBy { entry -> entry.id })
    }

    fun without(ids: Set<StandardId>): StandardsLock {
        val kept = entries.filterNot { entry -> entry.id in ids }
        return StandardsLock(kept)
    }
}
