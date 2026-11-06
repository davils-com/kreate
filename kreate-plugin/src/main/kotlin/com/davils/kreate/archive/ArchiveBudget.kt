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

package com.davils.kreate.archive

internal class ArchiveBudget(private val limits: ArchiveLimits = ArchiveLimits()) {
    var extractedBytes: Long = 0
        private set

    var entries: Int = 0
        private set

    private var nestingDepth: Int = 0

    fun chargeEntry() {
        entries++
        if (entries <= limits.maxEntries) return
        throw ArchiveException.LimitExceeded("The archive holds more than ${limits.maxEntries} entries.")
    }

    fun chargeBytes(bytes: Long) {
        val remaining = limits.maxExtractedBytes - extractedBytes
        if (bytes > remaining) {
            throw ArchiveException.LimitExceeded(
                "The archive expands to more than ${limits.maxExtractedBytes} bytes, nested archives included."
            )
        }
        extractedBytes += bytes
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
