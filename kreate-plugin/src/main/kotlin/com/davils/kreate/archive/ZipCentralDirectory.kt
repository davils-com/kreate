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

private const val END_RECORD_SIGNATURE: Long = 0x06054B50

private const val END_RECORD_SIZE: Int = 22

private const val END_DISK_NUMBER: Int = 4

private const val END_DIRECTORY_DISK: Int = 6

private const val END_DISK_ENTRIES: Int = 8

private const val END_TOTAL_ENTRIES: Int = 10

private const val END_DIRECTORY_SIZE: Int = 12

private const val END_DIRECTORY_OFFSET: Int = 16

private const val END_COMMENT_LENGTH: Int = 20

private const val MAX_COMMENT_LENGTH: Int = 0xFFFF

private const val ZIP64_LOCATOR_SIGNATURE: Long = 0x07064B50

private const val ZIP64_LOCATOR_SIZE: Int = 20

private const val ZIP64_ENTRY_COUNT: Int = 0xFFFF

private const val ZIP64_VALUE: Long = 0xFFFFFFFFL

private const val CENTRAL_SIGNATURE: Long = 0x02014B50

private const val CENTRAL_HEADER_SIZE: Int = 46

private const val CENTRAL_VERSION_MADE_BY: Int = 4

private const val CENTRAL_FLAGS: Int = 8

private const val CENTRAL_METHOD: Int = 10

private const val CENTRAL_CRC: Int = 16

private const val CENTRAL_COMPRESSED_SIZE: Int = 20

private const val CENTRAL_SIZE: Int = 24

private const val CENTRAL_NAME_LENGTH: Int = 28

private const val CENTRAL_EXTRA_LENGTH: Int = 30

private const val CENTRAL_COMMENT_LENGTH: Int = 32

private const val CENTRAL_EXTERNAL_ATTRIBUTES: Int = 38

private const val CENTRAL_LOCAL_OFFSET: Int = 42

internal class ZipCentralDirectory private constructor(
    val entries: List<ZipCentralEntry>,
    val archiveStart: Long,
    val directoryStart: Long
) {
    companion object {
        fun read(archive: LittleEndianBytes, budget: ArchiveBudget): ZipCentralDirectory {
            val endRecord = findEndRecord(archive)
            checkSupported(archive, endRecord)

            val directorySize = archive.uint32(endRecord + END_DIRECTORY_SIZE)
            val directoryStart = endRecord - directorySize
            val archiveStart = directoryStart - archive.uint32(endRecord + END_DIRECTORY_OFFSET)
            if (archiveStart < 0) malformed("The central directory lies outside the archive.")

            val expectedEntries = archive.uint16(endRecord + END_TOTAL_ENTRIES)
            val entries = readEntries(archive, budget, directoryStart, endRecord)
            if (entries.size != expectedEntries) malformed("The central directory holds a wrong entry count.")

            return ZipCentralDirectory(entries, archiveStart, directoryStart)
        }

        private fun findEndRecord(archive: LittleEndianBytes): Long {
            val last = (archive.size - END_RECORD_SIZE).toLong()
            val first = maxOf(0L, last - MAX_COMMENT_LENGTH)
            var offset = last
            while (offset >= first) {
                if (isEndRecord(archive, offset)) return offset
                offset--
            }
            malformed("The archive has no ZIP end record.")
        }

        private fun isEndRecord(archive: LittleEndianBytes, offset: Long): Boolean {
            if (archive.uint32(offset) != END_RECORD_SIGNATURE) return false

            val commentLength = archive.uint16(offset + END_COMMENT_LENGTH)
            return offset + END_RECORD_SIZE + commentLength == archive.size.toLong()
        }

        private fun checkSupported(archive: LittleEndianBytes, endRecord: Long) {
            val spansDisks = archive.uint16(endRecord + END_DISK_NUMBER) != 0 ||
                archive.uint16(endRecord + END_DIRECTORY_DISK) != 0
            if (spansDisks) malformed("Multi-disk ZIP archives are not supported.")

            val hasDiskEntryMismatch = archive.uint16(endRecord + END_DISK_ENTRIES) !=
                archive.uint16(endRecord + END_TOTAL_ENTRIES)
            if (hasDiskEntryMismatch) malformed("The ZIP end record contradicts itself.")

            if (usesZip64(archive, endRecord)) malformed("ZIP64 archives are not supported.")
        }

        private fun usesZip64(archive: LittleEndianBytes, endRecord: Long): Boolean {
            val locator = endRecord - ZIP64_LOCATOR_SIZE
            val hasLocator = locator >= 0 && archive.uint32(locator) == ZIP64_LOCATOR_SIGNATURE
            val hasMarkerValue = archive.uint16(endRecord + END_TOTAL_ENTRIES) == ZIP64_ENTRY_COUNT ||
                archive.uint32(endRecord + END_DIRECTORY_SIZE) == ZIP64_VALUE ||
                archive.uint32(endRecord + END_DIRECTORY_OFFSET) == ZIP64_VALUE
            return hasLocator || hasMarkerValue
        }

        private fun readEntries(
            archive: LittleEndianBytes,
            budget: ArchiveBudget,
            directoryStart: Long,
            directoryEnd: Long
        ): List<ZipCentralEntry> {
            val entries = ArrayList<ZipCentralEntry>()
            var offset = directoryStart
            while (offset < directoryEnd) {
                budget.chargeEntry()
                val entry = readEntry(archive, offset)
                entries.add(entry)
                offset += headerLength(archive, offset)
            }
            if (offset != directoryEnd) malformed("The central directory overruns its declared size.")
            return entries
        }

        private fun headerLength(archive: LittleEndianBytes, offset: Long): Long {
            val nameLength = archive.uint16(offset + CENTRAL_NAME_LENGTH)
            val extraLength = archive.uint16(offset + CENTRAL_EXTRA_LENGTH)
            val commentLength = archive.uint16(offset + CENTRAL_COMMENT_LENGTH)
            return (CENTRAL_HEADER_SIZE + nameLength + extraLength + commentLength).toLong()
        }

        private fun readEntry(archive: LittleEndianBytes, offset: Long): ZipCentralEntry {
            if (archive.uint32(offset) != CENTRAL_SIGNATURE) malformed("A central directory header is corrupt.")

            val nameLength = archive.uint16(offset + CENTRAL_NAME_LENGTH).toLong()
            val entry = ZipCentralEntry(
                nameBytes = archive.slice(offset + CENTRAL_HEADER_SIZE, nameLength),
                versionMadeBy = archive.uint16(offset + CENTRAL_VERSION_MADE_BY),
                flags = archive.uint16(offset + CENTRAL_FLAGS),
                method = archive.uint16(offset + CENTRAL_METHOD),
                crc = archive.uint32(offset + CENTRAL_CRC),
                compressedSize = archive.uint32(offset + CENTRAL_COMPRESSED_SIZE),
                size = archive.uint32(offset + CENTRAL_SIZE),
                externalAttributes = archive.uint32(offset + CENTRAL_EXTERNAL_ATTRIBUTES),
                localHeaderOffset = archive.uint32(offset + CENTRAL_LOCAL_OFFSET)
            )
            val hasMarkerValue = ZIP64_VALUE in listOf(entry.compressedSize, entry.size, entry.localHeaderOffset)
            if (hasMarkerValue) malformed("ZIP64 archives are not supported.")
            return entry
        }

        private fun malformed(message: String): Nothing = throw ArchiveException.Malformed(message)
    }
}
