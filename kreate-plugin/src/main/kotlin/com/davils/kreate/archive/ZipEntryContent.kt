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

import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

private const val LOCAL_SIGNATURE: Long = 0x04034B50

private const val LOCAL_HEADER_SIZE: Int = 30

private const val LOCAL_METHOD: Int = 8

private const val LOCAL_NAME_LENGTH: Int = 26

private const val LOCAL_EXTRA_LENGTH: Int = 28

private const val METHOD_STORED: Int = 0

private const val METHOD_DEFLATED: Int = 8

private const val ENCRYPTED_FLAG: Int = 1

private const val MAX_DEFLATE_RATIO: Long = 1032

private const val DEFLATE_SLACK_BYTES: Long = 1024

internal class ZipEntryContent(
    private val archive: LittleEndianBytes,
    private val directory: ZipCentralDirectory,
    private val budget: ArchiveBudget
) {
    fun read(entry: ZipCentralEntry): ByteArray {
        val compressed = compressedBytes(entry)
        budget.chargeBytes(entry.size)
        if (entry.size > Int.MAX_VALUE) {
            throw ArchiveException.LimitExceeded("The entry '${entry.name}' is too large to hold in memory.")
        }

        val content = decode(entry, compressed)
        checkCrc(entry, content)
        return content
    }

    private fun compressedBytes(entry: ZipCentralEntry): ByteArray {
        if (entry.flags and ENCRYPTED_FLAG != 0) malformed("The entry '${entry.name}' is encrypted.")

        val localHeader = directory.archiveStart + entry.localHeaderOffset
        if (localHeader + LOCAL_HEADER_SIZE > directory.directoryStart) {
            malformed("The entry '${entry.name}' lies outside the archive.")
        }
        if (archive.uint32(localHeader) != LOCAL_SIGNATURE) malformed("The entry '${entry.name}' has no local header.")

        checkLocalHeaderMatches(entry, localHeader)
        val nameLength = archive.uint16(localHeader + LOCAL_NAME_LENGTH)
        val extraLength = archive.uint16(localHeader + LOCAL_EXTRA_LENGTH)
        val dataStart = localHeader + LOCAL_HEADER_SIZE + nameLength + extraLength

        if (dataStart + entry.compressedSize > directory.directoryStart) {
            malformed("The entry '${entry.name}' lies outside the archive.")
        }
        return archive.slice(dataStart, entry.compressedSize)
    }

    private fun checkLocalHeaderMatches(entry: ZipCentralEntry, localHeader: Long) {
        val hasSameMethod = archive.uint16(localHeader + LOCAL_METHOD) == entry.method
        val nameLength = archive.uint16(localHeader + LOCAL_NAME_LENGTH).toLong()
        val localName = archive.slice(localHeader + LOCAL_HEADER_SIZE, nameLength)

        if (!hasSameMethod || !localName.contentEquals(entry.nameBytes)) {
            malformed("The local header of '${entry.name}' contradicts the central directory.")
        }
    }

    private fun decode(entry: ZipCentralEntry, compressed: ByteArray): ByteArray {
        when (entry.method) {
            METHOD_STORED -> return readStored(entry, compressed)
            METHOD_DEFLATED -> return inflate(entry, compressed)
        }
        malformed("The entry '${entry.name}' uses the unsupported compression method ${entry.method}.")
    }

    private fun readStored(entry: ZipCentralEntry, compressed: ByteArray): ByteArray {
        if (entry.compressedSize != entry.size) malformed("The stored entry '${entry.name}' has inconsistent sizes.")
        return compressed
    }

    private fun inflate(entry: ZipCentralEntry, compressed: ByteArray): ByteArray {
        val largestPossibleSize = compressed.size * MAX_DEFLATE_RATIO + DEFLATE_SLACK_BYTES
        if (entry.size > largestPossibleSize) malformed("The entry '${entry.name}' declares an impossible size.")

        val inflater = Inflater(true)
        try {
            inflater.setInput(compressed)
            return inflateAll(entry, inflater)
        } catch (cause: DataFormatException) {
            throw ArchiveException.Malformed("The entry '${entry.name}' holds corrupt data.", cause)
        } finally {
            inflater.end()
        }
    }

    private fun inflateAll(entry: ZipCentralEntry, inflater: Inflater): ByteArray {
        val content = ByteArray(entry.size.toInt())
        var written = 0
        while (written < content.size) {
            val inflated = inflater.inflate(content, written, content.size - written)
            if (inflated == 0) malformed("The entry '${entry.name}' is shorter than it declares.")
            written += inflated
        }

        val overrun = inflater.inflate(ByteArray(1))
        if (overrun > 0) malformed("The entry '${entry.name}' is longer than it declares.")
        return content
    }

    private fun checkCrc(entry: ZipCentralEntry, content: ByteArray) {
        val checksum = CRC32()
        checksum.update(content)
        if (checksum.value != entry.crc) malformed("The entry '${entry.name}' fails its checksum.")
    }

    private fun malformed(message: String): Nothing = throw ArchiveException.Malformed(message)
}
