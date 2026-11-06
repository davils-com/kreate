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
package com.davils.kreate.advisory.osv

import com.davils.kreate.advisory.version.MavenVersion

internal fun fixedVersionFor(installed: String, ranges: List<OsvRange>): String? {
    val version = MavenVersion(installed)
    val containing = ranges.firstOrNull { range -> range.contains(version) }
    if (containing != null) return containing.fixed

    val later = ranges.mapNotNull { range -> range.fixed }.filter { fixed -> MavenVersion(fixed) > version }
    return later.minWithOrNull(compareBy { fixed -> MavenVersion(fixed) })
}

private fun OsvRange.contains(version: MavenVersion): Boolean {
    val start = introduced?.let(::MavenVersion)
    val end = fixed?.let(::MavenVersion)
    val isAfterStart = start == null || version >= start
    val isBeforeEnd = end == null || version < end
    return isAfterStart && isBeforeEnd
}
