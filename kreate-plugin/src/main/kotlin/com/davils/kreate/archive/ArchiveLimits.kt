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

internal data class ArchiveLimits(
    val maxExtractedBytes: Long = DEFAULT_MAX_EXTRACTED_BYTES,
    val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    val maxNestingDepth: Int = DEFAULT_MAX_NESTING_DEPTH
) {
    companion object {
        const val DEFAULT_MAX_EXTRACTED_BYTES: Long = 512L * 1024 * 1024
        const val DEFAULT_MAX_ENTRIES: Int = 65_535
        const val DEFAULT_MAX_NESTING_DEPTH: Int = 4
    }
}
