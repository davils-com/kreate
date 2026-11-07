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
package com.davils.kreate.inspection.scan

import com.davils.kreate.archive.ArchiveBudget
import com.davils.kreate.archive.ArchiveEntry
import com.davils.kreate.archive.ArchiveException
import com.davils.kreate.archive.ZipArchiveReader

private const val MAX_RESOURCE_BYTES: Int = 256 * 1024

private const val CLASS_EXTENSION: String = ".class"

private val TEXT_EXTENSIONS: Set<String> = setOf(
    "properties", "xml", "json", "yaml", "yml", "txt",
    "sh", "bat", "cmd", "ps1", "py", "js",
    "kt", "java", "kts", "gradle"
)

internal class JarScanner {
    fun scan(jar: ByteArray, name: String): List<Hit> {
        val hits = mutableListOf<Hit>()
        scanArchive(jar, name, ArchiveBudget(), hits)
        return hits
    }

    private fun scanArchive(bytes: ByteArray, name: String, budget: ArchiveBudget, hits: MutableList<Hit>) {
        val structure = ArchiveStructureScanner(hits)
        val reader = ZipArchiveReader(budget)
        try {
            reader.forEachNestedEntry(bytes) { entry -> scanEntry(entry, name, budget, structure, hits) }
        } catch (cause: ArchiveException) {
            hits += Hit("MALFORMED_ARCHIVE", name, cause.message)
        }
    }

    private fun scanEntry(
        entry: ArchiveEntry,
        archiveName: String,
        budget: ArchiveBudget,
        structure: ArchiveStructureScanner,
        hits: MutableList<Hit>
    ) {
        structure.scan(entry)
        BuildTimeScanner.scan(entry, hits)
        if (!entry.isFile) return

        val name = entry.name
        val extension = name.substringAfterLast('.', "").lowercase()
        if (name.endsWith(CLASS_EXTENSION)) ClassCodeScanner.scan(entry.bytes, name, hits)
        if (extension in TEXT_EXTENSIONS) scanText(entry, hits)
        if (BinaryMagic.isArchive(entry.bytes)) scanArchive(entry.bytes, "$archiveName!$name", budget, hits)
    }

    private fun scanText(entry: ArchiveEntry, hits: MutableList<Hit>) {
        if (entry.bytes.size > MAX_RESOURCE_BYTES) return

        val text = entry.bytes.decodeToString()
        TextPatterns.scan(text, entry.name, hits)
        UnicodeTricks.scan(text, entry.name, hits)
    }
}
