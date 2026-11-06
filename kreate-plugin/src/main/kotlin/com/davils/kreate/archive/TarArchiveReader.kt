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

import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

private const val BLOCK_SIZE: Int = 512

internal class TarArchiveReader(private val budget: ArchiveBudget) {
    fun forEachEntry(archive: InputStream, action: (ArchiveEntry) -> Unit) {
        try {
            readEntries(archive, action)
        } catch (cause: IOException) {
            throw ArchiveException.Malformed("The tar archive could not be read: $cause", cause)
        }
    }

    fun forEachGzipEntry(archive: InputStream, action: (ArchiveEntry) -> Unit) {
        try {
            GZIPInputStream(archive).use { stream -> forEachEntry(stream, action) }
        } catch (cause: IOException) {
            throw ArchiveException.Malformed("The archive is not valid gzip data: $cause", cause)
        }
    }

    fun readAll(archive: InputStream): List<ArchiveEntry> = buildList { forEachEntry(archive, ::add) }

    private fun readEntries(archive: InputStream, action: (ArchiveEntry) -> Unit) {
        var block = readBlock(archive)
        while (!isZeroBlock(block)) {
            budget.chargeEntry()
            action(readEntry(archive, TarHeader.parse(block)))
            block = readBlock(archive)
        }

        val terminator = readBlock(archive)
        if (!isZeroBlock(terminator)) throw ArchiveException.Malformed("The tar archive ends in a data block.")
    }

    private fun readEntry(archive: InputStream, header: TarHeader): ArchiveEntry {
        val content = readContent(archive, header)
        return ArchiveEntry(header.name, header.kind, content, header.mode, header.linkTarget)
    }

    private fun readContent(archive: InputStream, header: TarHeader): ByteArray {
        budget.chargeBytes(header.size)
        if (header.size > Int.MAX_VALUE - BLOCK_SIZE) {
            throw ArchiveException.LimitExceeded("The tar entry '${header.name}' is too large to hold in memory.")
        }

        val size = header.size.toInt()
        val padding = (BLOCK_SIZE - size % BLOCK_SIZE) % BLOCK_SIZE
        val paddedContent = readExactly(archive, size + padding)
        return paddedContent.copyOf(size)
    }

    private fun readBlock(archive: InputStream): ByteArray {
        budget.chargeBytes(BLOCK_SIZE.toLong())
        return readExactly(archive, BLOCK_SIZE)
    }

    private fun readExactly(archive: InputStream, length: Int): ByteArray {
        val bytes = archive.readNBytes(length)
        if (bytes.size != length) throw ArchiveException.Malformed("The tar archive is truncated.")
        return bytes
    }

    private fun isZeroBlock(block: ByteArray): Boolean = block.all { byte -> byte == ZERO_BYTE }

    private companion object {
        private const val ZERO_BYTE: Byte = 0
    }
}
