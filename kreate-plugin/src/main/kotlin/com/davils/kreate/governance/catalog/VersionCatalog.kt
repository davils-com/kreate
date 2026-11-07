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
package com.davils.kreate.governance.catalog

import java.util.regex.Matcher

private val SECTION: Regex = Regex("^\\s*\\[([A-Za-z0-9_.-]+)]\\s*(#.*)?$")

private val ENTRY: Regex = Regex("^\\s*([A-Za-z0-9_.-]+)\\s*=\\s*(.+?)\\s*(#[^\"]*)?$")

private val STRING_NOTATION: Regex = Regex("^\"([^\":]+):([^\":]+)(?::([^\"]+))?\"$")

private val MODULE_KEY: Regex = Regex("""module\s*=\s*"([^":]+):([^"]+)"""")

private val GROUP_KEY: Regex = Regex("""group\s*=\s*"([^"]+)"""")

private val NAME_KEY: Regex = Regex("""(?<![.\w])name\s*=\s*"([^"]+)"""")

private val VERSION_KEY: Regex = Regex("""(?<![.\w])version\s*=\s*"([^"]*)"""")

private val VERSION_REF_KEY: Regex = Regex("""version\.ref\s*=\s*"([^"]+)"""")

private val QUOTED: Regex = Regex("\"[^\"]*\"")

private const val VERSIONS: String = "versions"

private const val LIBRARIES: String = "libraries"

internal class VersionCatalog(private val text: String) {
    private val lines: List<String> = text.lines()

    private val sections: List<String?> = sectionsOf(lines)

    val libraries: List<CatalogLibrary> by lazy {
        lines.indices.mapNotNull { index -> libraryAt(index) }
    }

    private val versions: Map<String, Pair<Int, String>> by lazy {
        lines.indices.mapNotNull { index -> versionAt(index) }.toMap()
    }

    fun libraryOf(group: String, name: String): CatalogLibrary? =
        libraries.firstOrNull { library -> library.group == group && library.name == name }

    fun libraryNamed(alias: String): CatalogLibrary? = libraries.firstOrNull { library -> library.alias == alias }

    fun hasAlias(alias: String): Boolean = libraries.any { library -> library.alias == alias }

    fun effectiveVersion(library: CatalogLibrary): String? {
        val ref = library.versionRef ?: return library.version
        return versions[ref]?.second
    }

    fun sharersOf(library: CatalogLibrary): List<String> {
        val ref = library.versionRef ?: return emptyList()
        val sharing = libraries.filter { other -> other.versionRef == ref && other.alias != library.alias }
        return sharing.map { other -> other.alias }
    }

    fun withVersion(library: CatalogLibrary, version: String): String {
        require(CatalogNames.isExactVersion(version)) { "$version is not an exact version." }
        val ref = library.versionRef
        if (ref != null) {
            val index = versions[ref]?.first ?: error("The version ${library.versionRef} is not declared.")
            return replaced(index, replaceFirstQuoted(lines[index], version))
        }
        val line = lines[library.line]
        val current = library.version ?: error("${library.alias} declares no version that can be raised.")
        val updated = line.replace("\"${library.module}:$current\"", "\"${library.module}:$version\"")
            .replace(Regex("(?<![.\\w])(version\\s*=\\s*)\"${Regex.escape(current)}\""), "$1\"${quote(version)}\"")
        return replaced(library.line, updated)
    }

    fun withLibrary(alias: String, group: String, name: String, version: String): String {
        require(CatalogNames.isAlias(alias)) { "$alias is no valid catalog alias." }
        val isModule = CatalogNames.isCoordinatePart(group) && CatalogNames.isCoordinatePart(name)
        require(isModule) { "$group:$name is no module." }
        require(CatalogNames.isExactVersion(version)) { "$version is not an exact version." }
        require(!hasAlias(alias)) { "The catalog already declares $alias." }

        val versionTaken = alias in versions
        val library = libraryLine(alias, "$group:$name", version, versionTaken)
        val withVersion = lines.takeIf { versionTaken } ?: inserted(lines, VERSIONS, "$alias = \"$version\"")
        return inserted(withVersion, LIBRARIES, library).joinToString("\n")
    }

    private fun libraryLine(alias: String, module: String, version: String, versionTaken: Boolean): String {
        if (versionTaken) return "$alias = { module = \"$module\", version = \"$version\" }"
        return "$alias = { module = \"$module\", version.ref = \"$alias\" }"
    }

    private fun libraryAt(index: Int): CatalogLibrary? {
        if (sections[index] != LIBRARIES) return null
        val entry = ENTRY.find(lines[index]) ?: return null
        val alias = entry.groupValues[1]
        val value = entry.groupValues[2]
        val notation = STRING_NOTATION.find(value)
        if (notation != null) {
            val (group, name, version) = notation.destructured
            return CatalogLibrary(alias, group, name, version.ifEmpty { null }, null, index)
        }
        return inlineLibrary(alias, value, index)
    }

    private fun inlineLibrary(alias: String, value: String, index: Int): CatalogLibrary? {
        val module = MODULE_KEY.find(value)
        val group = module?.groupValues?.get(1) ?: GROUP_KEY.find(value)?.groupValues?.get(1) ?: return null
        val name = module?.groupValues?.get(2) ?: NAME_KEY.find(value)?.groupValues?.get(1) ?: return null
        val version = VERSION_KEY.find(value)?.groupValues?.get(1)
        val ref = VERSION_REF_KEY.find(value)?.groupValues?.get(1)
        return CatalogLibrary(alias, group, name, version, ref, index)
    }

    private fun versionAt(index: Int): Pair<String, Pair<Int, String>>? {
        if (sections[index] != VERSIONS) return null
        val entry = ENTRY.find(lines[index]) ?: return null
        val value = entry.groupValues[2]
        if (!value.startsWith("\"")) return null
        return entry.groupValues[1] to (index to value.trim('"'))
    }

    private fun replaced(index: Int, line: String): String {
        val updated = lines.toMutableList()
        updated[index] = line
        return updated.joinToString("\n")
    }

    private fun replaceFirstQuoted(line: String, version: String): String {
        val separator = line.indexOf('=')
        val head = line.substring(0, separator + 1)
        val tail = line.substring(separator + 1)
        return head + QUOTED.replaceFirst(tail, Matcher.quoteReplacement("\"$version\""))
    }

    private fun quote(value: String): String = Matcher.quoteReplacement(value)

    private companion object {
        fun sectionsOf(lines: List<String>): List<String?> {
            var current: String? = null
            return lines.map { line ->
                SECTION.find(line)?.let { header -> current = header.groupValues[1] }
                current
            }
        }

        fun inserted(lines: List<String>, section: String, entry: String): List<String> {
            val sections = sectionsOf(lines)
            val header = sections.indexOfFirst { name -> name == section }
            if (header < 0) return appendedSection(lines, section, entry)

            val members = sections.indices.filter { index -> sections[index] == section }
            val last = members.lastOrNull { index -> lines[index].isNotBlank() } ?: header
            val updated = lines.toMutableList()
            updated.add(last + 1, entry)
            return updated
        }

        private fun appendedSection(lines: List<String>, section: String, entry: String): List<String> {
            val content = lines.dropLastWhile { line -> line.isBlank() }
            val blank = listOf("")
            val separator = blank.takeIf { content.isNotEmpty() }.orEmpty()
            if (section == VERSIONS) return listOf("[$section]", entry, "") + content + listOf("")
            return content + separator + listOf("[$section]", entry, "")
        }
    }
}
