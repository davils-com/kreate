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

package com.davils.kreate.net

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.readByteArray
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import io.ktor.http.HttpMethod as KtorHttpMethod

private const val DOWNLOAD_CHUNK_BYTES: Long = 1024L * 1024L

internal class KtorHttpTransport(
    private val client: HttpClient,
    private val userAgent: String,
    private val timeout: Duration,
    private val clock: Clock
) : HttpTransport {
    override fun send(request: TransportRequest): TransportResponse = try {
        runBlocking { exchangeWithin(timeout, request) }
    } catch (cause: TimeoutCancellationException) {
        throw NetworkException.Unavailable("The request to ${request.uri.host} timed out.", cause)
    } catch (cause: IOException) {
        throw NetworkException.Unavailable("The request to ${request.uri.host} failed: $cause", cause)
    } catch (cause: InterruptedException) {
        Thread.currentThread().interrupt()
        throw NetworkException.Interrupted("Interrupted while calling ${request.uri.host}.", cause)
    }

    override fun close() {
        client.close()
    }

    private suspend fun exchangeWithin(limit: Duration, request: TransportRequest): TransportResponse =
        withTimeout(limit.toMillis()) {
            val statement = client.prepareRequest { describe(request) }
            statement.execute { response -> readResponse(request, response) }
        }

    private fun HttpRequestBuilder.describe(request: TransportRequest) {
        url(Url(request.uri))
        method = KtorHttpMethod.parse(request.method.name)
        header(HttpHeaders.UserAgent, userAgent)

        val explicitHeaders = request.headers.filterKeys { name -> !isContentType(name) }
        explicitHeaders.forEach { (name, value) -> headers[name] = value }

        val body = request.body ?: return
        setBody(ByteArrayContent(body, contentTypeOf(request)))
    }

    private fun contentTypeOf(request: TransportRequest): ContentType? {
        val declared = request.headers.entries.firstOrNull { (name, _) -> isContentType(name) }
        return declared?.let { (_, value) -> ContentType.parse(value) }
    }

    private fun isContentType(name: String): Boolean = name.equals(HttpHeaders.ContentType, ignoreCase = true)

    private suspend fun readResponse(request: TransportRequest, response: HttpResponse): TransportResponse {
        val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (request.method != HttpMethod.HEAD && declaredLength != null) {
            checkDeclaredLength(request, declaredLength)
        }

        val bytes = readBounded(request, response)
        val headers = response.headers.toMap()
        return TransportResponse(request.uri, response.status.value, headers, bytes, clock.instant())
    }

    private fun checkDeclaredLength(request: TransportRequest, declaredLength: Long) {
        if (declaredLength <= request.maxBytes) return
        throw tooLarge(request)
    }

    private suspend fun readBounded(request: TransportRequest, response: HttpResponse): ByteArray {
        val channel = response.bodyAsChannel()
        val target = request.downloadTo
        if (target != null && response.status.isSuccess()) {
            writeBounded(request, channel, target)
            return ByteArray(0)
        }

        val packet = channel.readRemaining(request.maxBytes + 1L)
        val bytes = packet.readByteArray()

        if (bytes.size > request.maxBytes) throw tooLarge(request)
        return bytes
    }

    private suspend fun writeBounded(request: TransportRequest, channel: ByteReadChannel, target: Path) {
        Files.newOutputStream(target).use { output ->
            var received = 0L
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DOWNLOAD_CHUNK_BYTES)
                val chunk = packet.readByteArray()
                received += chunk.size
                if (received > request.maxBytes) throw tooLarge(request)
                output.write(chunk)
            }
        }
    }

    private fun tooLarge(request: TransportRequest): NetworkException.TooLarge =
        NetworkException.TooLarge("The response of ${request.uri.host} exceeds ${request.maxBytes} bytes.")
}
