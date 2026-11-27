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
package com.davils.kreate.standard.provider.suite

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kreate.archive.ArchiveBudget
import com.davils.kreate.archive.ArchiveEntry
import com.davils.kreate.archive.ArchiveException
import com.davils.kreate.archive.TarArchiveReader
import com.davils.kreate.archive.ZipArchiveReader
import com.davils.kreate.digest.Sha256
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.fetch.FetchedDocument
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.provider.ProviderContext
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URI
import java.nio.file.Files

private const val MAX_ARCHIVE_BYTES: Int = 512 * 1024 * 1024

private const val MAGIC_BYTES: Int = 4

private const val GZIP_FIRST: Int = 0x1f

private const val GZIP_SECOND: Int = 0x8b

private const val ZIP_FIRST: Int = 'P'.code

private const val ZIP_SECOND: Int = 'K'.code

private const val CURRENT_DIRECTORY: String = "./"

private const val BYTE_MASK: Int = 0xff

internal class ArchiveIndexer(private val context: ProviderContext) {
    fun indexOf(archive: String, sha256: String, root: String?): Outcome<SuiteIndex, LookupFailure> {
        val fetched = context.fetch(FetchRequest(URI(archive), MAX_ARCHIVE_BYTES, isBulk = true))
        return fetched.flatMap { document -> verified(archive, sha256, root, document) }
    }

    private fun verified(
        archive: String,
        sha256: String,
        root: String?,
        document: FetchedDocument
    ): Outcome<SuiteIndex, LookupFailure> {
        val actual = try {
            sha256Of(document)
        } catch (cause: IOException) {
            return unavailable("The archive $archive could not be read", cause)
        }
        if (actual != sha256) {
            val reason = "The archive $archive has the sha256 $actual, not the pinned $sha256."
            return Outcome.Failure(LookupFailure.Mismatch(reason))
        }
        return entriesOf(archive, document, root)
    }

    private fun entriesOf(
        archive: String,
        document: FetchedDocument,
        root: String?
    ): Outcome<SuiteIndex, LookupFailure> {
        val digests = mutableMapOf<String, String>()
        val unsafe = mutableListOf<String>()
        val failure = readFailure(archive) { forEachEntry(document) { entry -> collect(entry, digests, unsafe) } }
        if (failure != null) return Outcome.Failure(failure)
        val first = unsafe.firstOrNull()
        val reason = "The archive $archive holds the unsafe path $first."
        if (first != null) return Outcome.Failure(LookupFailure.Mismatch(reason))
        return Outcome.Success(SuiteIndex.rooted(digests, root, isGitBlob = false))
    }

    private fun readFailure(archive: String, read: () -> Unit): LookupFailure? = try {
        read()
        null
    } catch (cause: ArchiveException.LimitExceeded) {
        LookupFailure.Unavailable("The archive $archive is too large to check: ${cause.message.orEmpty()}")
    } catch (cause: ArchiveException.Malformed) {
        LookupFailure.Mismatch("The archive $archive cannot be read: ${cause.message.orEmpty()}")
    } catch (cause: IOException) {
        LookupFailure.Unavailable("The archive $archive could not be read: ${cause.message.orEmpty()}")
    }

    private fun unavailable(reason: String, cause: Exception): Outcome<SuiteIndex, LookupFailure> =
        Outcome.Failure(LookupFailure.Unavailable("$reason: ${cause.message.orEmpty()}"))

    private fun collect(entry: ArchiveEntry, digests: MutableMap<String, String>, unsafe: MutableList<String>) {
        if (entry.pathIssue != null) {
            unsafe += entry.name
            return
        }
        if (!entry.isFile) return
        val name = entry.name.removePrefix(CURRENT_DIRECTORY)
        digests[name] = Sha256.hexOf(entry.bytes)
    }

    private fun forEachEntry(document: FetchedDocument, action: (ArchiveEntry) -> Unit) {
        val budget = ArchiveBudget()
        val magic = Files.newInputStream(document.path).use { stream -> stream.readNBytes(MAGIC_BYTES) }
        if (isZip(magic)) {
            ZipArchiveReader(budget).forEachEntry(document.bytes(), action)
            return
        }
        val reader = TarArchiveReader(budget)
        Files.newInputStream(document.path).use { raw ->
            val stream = BufferedInputStream(raw)
            if (isGzip(magic)) {
                reader.forEachGzipEntry(stream, action)
                return
            }
            reader.forEachEntry(stream, action)
        }
    }

    private fun isGzip(magic: ByteArray): Boolean = startsWith(magic, GZIP_FIRST, GZIP_SECOND)

    private fun isZip(magic: ByteArray): Boolean = startsWith(magic, ZIP_FIRST, ZIP_SECOND)

    private fun startsWith(magic: ByteArray, first: Int, second: Int): Boolean {
        if (magic.size < 2) return false
        val firstByte = magic[0].toInt() and BYTE_MASK
        val secondByte = magic[1].toInt() and BYTE_MASK
        return firstByte == first && secondByte == second
    }

    private fun sha256Of(document: FetchedDocument): String = Sha256.hexOf(document.path)
}
