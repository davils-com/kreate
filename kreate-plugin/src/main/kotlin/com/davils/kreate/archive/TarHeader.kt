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

private const val NAME_OFFSET: Int = 0

private const val NAME_LENGTH: Int = 100

private const val MODE_OFFSET: Int = 100

private const val MODE_LENGTH: Int = 8

private const val SIZE_OFFSET: Int = 124

private const val SIZE_LENGTH: Int = 12

private const val CHECKSUM_OFFSET: Int = 148

private const val CHECKSUM_LENGTH: Int = 8

private const val TYPE_OFFSET: Int = 156

private const val LINK_NAME_OFFSET: Int = 157

private const val LINK_NAME_LENGTH: Int = 100

private const val MAGIC_OFFSET: Int = 257

private const val MAGIC_LENGTH: Int = 6

private const val POSIX_MAGIC: String = "ustar\u0000"

private const val PREFIX_OFFSET: Int = 345

private const val PREFIX_LENGTH: Int = 155

private const val BYTE_MASK: Int = 0xFF

private const val SPACE_CODE: Int = 0x20

private const val OCTAL_BITS: Int = 3

private const val NUL: Char = '\u0000'

private val OCTAL_DIGITS: CharRange = '0'..'7'

private val TYPE_KINDS: Map<Char, ArchiveEntryKind> = mapOf(
    NUL to ArchiveEntryKind.FILE,
    '0' to ArchiveEntryKind.FILE,
    '7' to ArchiveEntryKind.FILE,
    '1' to ArchiveEntryKind.HARD_LINK,
    '2' to ArchiveEntryKind.SYMBOLIC_LINK,
    '3' to ArchiveEntryKind.SPECIAL,
    '4' to ArchiveEntryKind.SPECIAL,
    '5' to ArchiveEntryKind.DIRECTORY,
    '6' to ArchiveEntryKind.SPECIAL
)

internal class TarHeader private constructor(
    val name: String,
    val kind: ArchiveEntryKind,
    val mode: Int,
    val size: Long,
    val linkTarget: String?
) {
    companion object {
        fun parse(block: ByteArray): TarHeader {
            checkChecksum(block)

            val type = Char(unsigned(block[TYPE_OFFSET]))
            val kind = TYPE_KINDS[type] ?: malformed("The tar entry type '$type' is not supported.")
            val size = parseOctal(block, SIZE_OFFSET, SIZE_LENGTH)
            if (kind != ArchiveEntryKind.FILE && size != 0L) malformed("A tar link or directory declares content.")

            val linkTarget = text(block, LINK_NAME_OFFSET, LINK_NAME_LENGTH).takeIf { it.isNotEmpty() }
            val mode = parseOctal(block, MODE_OFFSET, MODE_LENGTH).toInt()
            return TarHeader(nameOf(block), kind, mode, size, linkTarget)
        }

        private fun nameOf(block: ByteArray): String {
            val name = text(block, NAME_OFFSET, NAME_LENGTH)
            val magic = String(block, MAGIC_OFFSET, MAGIC_LENGTH, Charsets.ISO_8859_1)
            if (magic != POSIX_MAGIC) return name

            val prefix = text(block, PREFIX_OFFSET, PREFIX_LENGTH)
            if (prefix.isEmpty()) return name
            return "$prefix/$name"
        }

        private fun checkChecksum(block: ByteArray) {
            val stored = parseOctal(block, CHECKSUM_OFFSET, CHECKSUM_LENGTH)
            val checksumField = CHECKSUM_OFFSET until CHECKSUM_OFFSET + CHECKSUM_LENGTH
            val headerSum = block.indices.filter { index -> index !in checksumField }
                .sumOf { index -> unsigned(block[index]) }
            val computed = headerSum + CHECKSUM_LENGTH * SPACE_CODE
            if (stored != computed.toLong()) malformed("A tar header fails its checksum.")
        }

        private fun unsigned(byte: Byte): Int = byte.toInt() and BYTE_MASK

        private fun parseOctal(block: ByteArray, offset: Int, length: Int): Long {
            val field = String(block, offset, length, Charsets.ISO_8859_1)
            val unpadded = field.trimStart(' ')
            val digits = unpadded.substringBefore(NUL).trimEnd(' ')
            if (!digits.all { digit -> digit in OCTAL_DIGITS }) {
                malformed("A tar header holds an unsupported numeric field.")
            }
            return digits.fold(0L) { value, digit -> (value shl OCTAL_BITS) + digit.digitToInt() }
        }

        private fun text(block: ByteArray, offset: Int, length: Int): String {
            val field = String(block, offset, length, Charsets.UTF_8)
            return field.substringBefore(NUL)
        }

        private fun malformed(message: String): Nothing = throw ArchiveException.Malformed(message)
    }
}
