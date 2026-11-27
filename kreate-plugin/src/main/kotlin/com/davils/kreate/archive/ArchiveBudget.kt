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

@file:OptIn(DavilsInternal::class)

package com.davils.kreate.archive

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

private const val ENTRIES: String = "archive entries"
private const val EXTRACTED_BYTES: String = "extracted archive bytes"

internal class ArchiveBudget(private val limits: ArchiveLimits = ArchiveLimits()) {
    private val entryBudget = WorkBudget(maximum = limits.maxEntries.toLong(), limit = ENTRIES)
    private val byteBudget = WorkBudget(maximum = limits.maxExtractedBytes, limit = EXTRACTED_BYTES)
    private var nestingDepth: Int = 0

    val extractedBytes: Long get() = byteBudget.spent

    val entries: Int get() = entryBudget.spent.toInt()

    fun chargeEntry() {
        if (entryBudget.tryCharge()) return
        throw ArchiveException.LimitExceeded("The archive holds more than ${limits.maxEntries} entries.")
    }

    fun chargeBytes(bytes: Long) {
        if (bytes < 0L) throw ArchiveException.Malformed("An archive entry declares a negative size.")
        if (byteBudget.tryCharge(bytes)) return
        throw ArchiveException.LimitExceeded(
            "The archive expands to more than ${limits.maxExtractedBytes} bytes, nested archives included."
        )
    }

    fun <T> nested(read: () -> T): T {
        if (nestingDepth >= limits.maxNestingDepth) {
            throw ArchiveException.LimitExceeded("Archives nest deeper than ${limits.maxNestingDepth} levels.")
        }

        nestingDepth++
        try {
            return read()
        } finally {
            nestingDepth--
        }
    }
}
