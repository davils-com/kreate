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
package com.davils.kreate.changelog.document

private const val DEFAULT_HEADER: String = "# Changelog\n"

internal object ChangelogWriter {
    fun insert(text: String, version: String, section: String): String {
        val document = ChangelogDocument.parse(text)
        require(document.find(version) == null) { "The changelog already has a section for $version." }
        val lines = text.replace("\r\n", "\n").split('\n')
        val releases = document.headings.filterNot { heading -> heading.isUnreleased }
        val anchor = releases.firstOrNull()?.line
        if (text.isBlank()) return DEFAULT_HEADER + "\n" + section
        if (anchor == null) return text.trimEnd() + "\n\n" + section
        val before = lines.subList(0, anchor - 1)
        val after = lines.subList(anchor - 1, lines.size)
        return (before + section.trimEnd().split('\n') + "" + after).joinToString("\n")
    }
}
