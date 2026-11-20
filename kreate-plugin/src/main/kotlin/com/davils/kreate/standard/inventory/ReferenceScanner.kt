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
package com.davils.kreate.standard.inventory

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardLinks
import com.davils.kreate.standard.model.W3cShortnames
import com.davils.kreate.standard.rule.StandardFindings
import com.davils.kreate.standard.rule.StandardRules
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

private const val MAX_FILE_BYTES: Long = 4L * 1024 * 1024

private const val MAX_SCANNED_LINE_LENGTH: Int = 4096

private const val BINARY_PROBE_BYTES: Int = 8192

private const val NUL: Byte = 0

internal class ReferenceScanner(private val declared: Set<StandardId>) {
    fun findingsOf(files: List<ScannedFile>): List<Finding> {
        val references = files.sortedBy { file -> file.path }.flatMap { file -> referencesIn(file) }
        val (versionless, versioned) = references.partition { reference -> isVersionless(reference.id) }
        val undeclared = versioned.filterNot { reference -> reference.id in declared }
        val byId = undeclared.groupBy { reference -> reference.id }
        val ambiguous = versionless.groupBy { reference -> reference.id }
        val undeclaredFindings = byId.keys.sorted().map { id -> findingOf(id, byId.getValue(id)) }
        val ambiguousFindings = ambiguous.keys.sorted().map { id -> ambiguousFindingOf(id, ambiguous.getValue(id)) }
        return undeclaredFindings + ambiguousFindings
    }

    private fun isVersionless(id: StandardId): Boolean = W3cShortnames.versionsOf(id).isNotEmpty()

    private fun ambiguousFindingOf(id: StandardId, references: List<Reference>): Finding {
        val first = references.first()
        val versions = W3cShortnames.versionsOf(id).joinToString(" or ")
        return StandardFindings.findingOf(
            ruleId = StandardRules.VERSIONLESS_RULE,
            id = id,
            title = "${first.path} links $id, which names no version; link and declare $versions",
            url = StandardLinks.infoUrlOf(id),
            attributes = mapOf("occurrences" to references.size.toString(), "versions" to versions),
            location = SourceLocation(first.path, first.line)
        )
    }

    private fun referencesIn(file: ScannedFile): List<Reference> {
        val text = textOf(file.file) ?: return emptyList()
        val lines = text.lineSequence()
        val indexed = lines.withIndex()
        return indexed.flatMap { (index, line) -> referencesIn(line, file.path, index + 1) }.toList()
    }

    private fun referencesIn(line: String, path: String, number: Int): List<Reference> {
        val scanned = line.take(MAX_SCANNED_LINE_LENGTH)
        val ids = ReferencePatterns.idsIn(scanned)
        return ids.map { id -> Reference(id, path, number) }
    }

    private fun findingOf(id: StandardId, references: List<Reference>): Finding {
        val first = references.first()
        val files = references.map { reference -> reference.path }.distinct()
        return StandardFindings.findingOf(
            ruleId = StandardRules.UNDECLARED_RULE,
            id = id,
            title = "${first.path} names $id, which assurance { standards { } } does not declare",
            url = StandardLinks.infoUrlOf(id),
            attributes = mapOf("occurrences" to references.size.toString(), "files" to files.size.toString()),
            location = SourceLocation(first.path, first.line)
        )
    }

    private fun textOf(file: File): String? {
        if (!file.isFile || file.length() > MAX_FILE_BYTES) return null
        val bytes = file.readBytes()
        val probe = bytes.take(BINARY_PROBE_BYTES)
        if (NUL in probe) return null
        val decoder = StandardCharsets.UTF_8.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPLACE)
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE)
        val decoded = decoder.decode(ByteBuffer.wrap(bytes))
        return decoded.toString()
    }

    private class Reference(val id: StandardId, val path: String, val line: Int)
}
