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

import com.davils.kreate.archive.ArchiveEntry
import com.davils.kreate.archive.ArchivePathIssue

private const val MAX_NAME_LENGTH: Int = 4096

private val SCRIPT_EXTENSIONS: Set<String> = setOf("sh", "bat", "cmd", "ps1", "py", "rb", "js")

private val NESTED_ARCHIVE_EXTENSIONS: Set<String> = setOf("jar", "war", "zip", "aar")

internal class ArchiveStructureScanner(private val hits: MutableCollection<Hit>) {
    private val names: MutableSet<String> = hashSetOf()

    private val lowerCaseNames: MutableMap<String, String> = hashMapOf()

    fun scan(entry: ArchiveEntry) {
        val name = entry.name
        checkPath(entry)
        if (name.length > MAX_NAME_LENGTH) hits += Hit("OVERLONG_NAME", name.take(MAX_NAME_LENGTH))
        checkCollisions(name)
        if (!entry.isFile) return

        checkContent(name, entry.bytes)
    }

    private fun checkPath(entry: ArchiveEntry) {
        when (entry.pathIssue) {
            ArchivePathIssue.ABSOLUTE -> hits += Hit("ABSOLUTE_PATH", entry.name)
            ArchivePathIssue.TRAVERSAL -> hits += Hit("PATH_TRAVERSAL", entry.name)
            ArchivePathIssue.CONTROL_CHARACTER -> hits += Hit("PATH_TRAVERSAL", entry.name)
            null -> return
        }
    }

    private fun checkCollisions(name: String) {
        if (!names.add(name)) {
            hits += Hit("DUPLICATE_ENTRY", name)
            return
        }

        val lowerCase = name.lowercase()
        val existing = lowerCaseNames.putIfAbsent(lowerCase, name)
        if (existing != null) hits += Hit("CASE_COLLISION", name)
    }

    private fun checkContent(name: String, bytes: ByteArray) {
        val fileName = name.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (fileName.startsWith(".") && extension in SCRIPT_EXTENSIONS) hits += Hit("HIDDEN_EXECUTABLE", name)
        if (BinaryMagic.isNative(name, bytes)) hits += Hit("NATIVE_BINARY", name)
        if (extension in NESTED_ARCHIVE_EXTENSIONS) hits += Hit("NESTED_ARCHIVE", name)
    }
}
