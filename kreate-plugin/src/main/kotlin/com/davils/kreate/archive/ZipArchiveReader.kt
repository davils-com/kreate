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

private const val MODE_BITS: Int = 0xFFF

private const val FILE_TYPE_MASK: Int = 0xF000

private const val FILE_TYPE_REGULAR: Int = 0x8000

private const val FILE_TYPE_DIRECTORY: Int = 0x4000

private const val FILE_TYPE_SYMBOLIC_LINK: Int = 0xA000

private const val UNIX_ATTRIBUTES_SHIFT: Int = 16

private const val MADE_BY_HOST_SHIFT: Int = 8

private const val MADE_BY_UNIX_HOST: Int = 3

private const val DIRECTORY_NAME_SUFFIX: String = "/"

internal class ZipArchiveReader(private val budget: ArchiveBudget) {
    fun forEachEntry(archive: ByteArray, action: (ArchiveEntry) -> Unit) {
        val bytes = LittleEndianBytes(archive)
        val directory = ZipCentralDirectory.read(bytes, budget)
        val content = ZipEntryContent(bytes, directory, budget)

        directory.entries.forEach { entry -> action(toArchiveEntry(entry, content.read(entry))) }
    }

    fun forEachNestedEntry(archive: ByteArray, action: (ArchiveEntry) -> Unit) {
        budget.nested { forEachEntry(archive, action) }
    }

    fun readAll(archive: ByteArray): List<ArchiveEntry> = buildList { forEachEntry(archive, ::add) }

    private fun toArchiveEntry(entry: ZipCentralEntry, content: ByteArray): ArchiveEntry {
        val unixMode = unixModeOf(entry)
        val kind = kindOf(entry, unixMode)
        val linkTarget = content.toString(Charsets.UTF_8).takeIf { kind == ArchiveEntryKind.SYMBOLIC_LINK }
        return ArchiveEntry(entry.name, kind, content, unixMode and MODE_BITS, linkTarget)
    }

    private fun unixModeOf(entry: ZipCentralEntry): Int {
        val isUnix = entry.versionMadeBy shr MADE_BY_HOST_SHIFT == MADE_BY_UNIX_HOST
        if (!isUnix) return 0
        return (entry.externalAttributes shr UNIX_ATTRIBUTES_SHIFT).toInt()
    }

    private fun kindOf(entry: ZipCentralEntry, unixMode: Int): ArchiveEntryKind {
        if (entry.name.endsWith(DIRECTORY_NAME_SUFFIX)) return ArchiveEntryKind.DIRECTORY

        when (unixMode and FILE_TYPE_MASK) {
            0, FILE_TYPE_REGULAR -> return ArchiveEntryKind.FILE
            FILE_TYPE_DIRECTORY -> return ArchiveEntryKind.DIRECTORY
            FILE_TYPE_SYMBOLIC_LINK -> return ArchiveEntryKind.SYMBOLIC_LINK
        }
        return ArchiveEntryKind.SPECIAL
    }
}
