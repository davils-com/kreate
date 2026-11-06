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

private const val PARENT_SEGMENT: String = ".."

private val DRIVE_PREFIX: Regex = Regex("^[A-Za-z]:")

private val SEGMENT_SEPARATOR: Regex = Regex("""[/\\]""")

internal object ArchivePathInspector {
    fun inspect(path: String): ArchivePathIssue? {
        if (path.any { character -> character < ' ' }) return ArchivePathIssue.CONTROL_CHARACTER
        if (isAbsolute(path)) return ArchivePathIssue.ABSOLUTE
        if (hasParentSegment(path)) return ArchivePathIssue.TRAVERSAL
        return null
    }

    private fun isAbsolute(path: String): Boolean {
        val startsAtRoot = path.startsWith('/') || path.startsWith('\\')
        return startsAtRoot || DRIVE_PREFIX.containsMatchIn(path)
    }

    private fun hasParentSegment(path: String): Boolean {
        val segments = SEGMENT_SEPARATOR.split(path)
        return PARENT_SEGMENT in segments
    }
}
