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
package com.davils.kreate.standard.fetch

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.SafeText
import com.davils.kreate.digest.Sha256
import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.net.TransportResponse
import com.davils.kreate.standard.model.LookupFailure
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Properties

private const val HTTP_NOT_MODIFIED: Int = 304

private const val ETAG: String = "etag"

private const val LAST_MODIFIED: String = "last-modified"

internal class RevalidatingCache(
    private val directory: Path,
    private val timeToLive: Duration,
    private val clock: Clock
) {
    private var oldest: Instant? = null

    val oldestAnswer: Instant?
        get() = oldest

    fun fetch(request: FetchRequest, transport: HttpTransport): Outcome<FetchedDocument, LookupFailure> {
        val body = fileOf(request.uri, ".body")
        val written = modifiedAt(body)
        if (written != null && isFresh(written)) return answered(body, written)
        val fetched = try {
            revalidate(request, transport, body)
        } catch (cause: NetworkException) {
            unavailable(request.uri, cause.message.orEmpty())
        } catch (cause: IOException) {
            unavailable(request.uri, "the cache could not be written: ${cause.message.orEmpty()}")
        }
        if (fetched !is Outcome.Failure || fetched.failure !is LookupFailure.Unavailable) return fetched
        if (written == null) return fetched
        return answered(body, written)
    }

    private fun revalidate(
        request: FetchRequest,
        transport: HttpTransport,
        body: Path
    ): Outcome<FetchedDocument, LookupFailure> {
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, ".download", ".tmp")
        try {
            val response = transport.send(transportRequestOf(request, body, temporary))
            return accepted(request.uri, response, body, temporary)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun transportRequestOf(request: FetchRequest, body: Path, temporary: Path): TransportRequest {
        val validators = validatorsOf(body)
        val host = request.uri.host.orEmpty()
        return TransportRequest(
            method = HttpMethod.GET,
            uri = request.uri,
            headers = request.headers + validators,
            maxBytes = request.maxBytes,
            allowedHosts = request.redirectHosts + host,
            downloadTo = temporary
        )
    }

    private fun accepted(
        uri: URI,
        response: TransportResponse,
        body: Path,
        temporary: Path
    ): Outcome<FetchedDocument, LookupFailure> {
        val isUnchanged = response.statusCode == HTTP_NOT_MODIFIED && Files.isRegularFile(body)
        if (isUnchanged) return stored(body)
        if (response.isNotFound) return Outcome.Failure(LookupFailure.Unknown("${shown(uri)} does not exist."))
        if (!response.isSuccess) return unavailable(uri, "it answered with HTTP ${response.statusCode}")
        Files.move(temporary, body, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        writeValidators(body, response)
        return stored(body)
    }

    private fun stored(body: Path): Outcome<FetchedDocument, LookupFailure> {
        val now = clock.instant()
        Files.setLastModifiedTime(body, FileTime.from(now))
        return answered(body, now)
    }

    private fun answered(body: Path, at: Instant): Outcome<FetchedDocument, LookupFailure> {
        val earliest = oldest
        if (earliest == null || at.isBefore(earliest)) oldest = at
        return Outcome.Success(FetchedDocument(body, at))
    }

    private fun unavailable(uri: URI, reason: String): Outcome<FetchedDocument, LookupFailure> =
        Outcome.Failure(LookupFailure.Unavailable("${shown(uri)} could not be read: ${SafeText.text(reason)}"))

    private fun isFresh(written: Instant): Boolean {
        val age = Duration.between(written, clock.instant())
        return !age.isNegative && age <= timeToLive
    }

    private fun modifiedAt(file: Path): Instant? {
        if (!Files.isRegularFile(file)) return null
        val time = Files.getLastModifiedTime(file)
        return time.toInstant()
    }

    private fun validatorsOf(body: Path): Map<String, String> {
        val meta = metaOf(body)
        if (!Files.isRegularFile(body) || !Files.isRegularFile(meta)) return emptyMap()
        val properties = Properties()
        Files.newBufferedReader(meta).use { reader -> properties.load(reader) }
        val etag = properties.getProperty(ETAG)?.let { value -> "If-None-Match" to value }
        val modified = properties.getProperty(LAST_MODIFIED)?.let { value -> "If-Modified-Since" to value }
        return listOfNotNull(etag, modified).toMap()
    }

    private fun writeValidators(body: Path, response: TransportResponse) {
        val properties = Properties()
        response.header(ETAG)?.let { value -> properties.setProperty(ETAG, value) }
        response.header(LAST_MODIFIED)?.let { value -> properties.setProperty(LAST_MODIFIED, value) }
        Files.newBufferedWriter(metaOf(body)).use { writer -> properties.store(writer, null) }
    }

    private fun metaOf(body: Path): Path = body.resolveSibling(body.fileName.toString().replace(".body", ".meta"))

    private fun fileOf(uri: URI, suffix: String): Path = directory.resolve(Sha256.hexOf(uri.toString()) + suffix)

    private fun shown(uri: URI): String = SafeText.text(uri.toString())
}
