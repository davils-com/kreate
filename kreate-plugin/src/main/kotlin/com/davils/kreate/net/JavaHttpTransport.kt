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

import java.io.IOException
import java.io.InputStream
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private const val USER_AGENT_HEADER: String = "User-Agent"

private const val CONTENT_LENGTH_HEADER: String = "content-length"

internal class JavaHttpTransport(
    private val client: HttpClient,
    private val userAgent: String,
    private val timeout: Duration,
    private val clock: Clock
) : HttpTransport {
    override fun send(request: TransportRequest): TransportResponse {
        val response = exchange(request)
        return response.body().use { body -> readResponse(request, response, body) }
    }

    private fun exchange(request: TransportRequest): HttpResponse<InputStream> {
        val exchange = toHttpRequest(request)
        val bodyHandler = HttpResponse.BodyHandlers.ofInputStream()

        return try {
            client.send(exchange, bodyHandler)
        } catch (cause: IOException) {
            throw NetworkException.Unavailable("The request to ${request.uri.host} failed: $cause", cause)
        } catch (cause: InterruptedException) {
            Thread.currentThread().interrupt()
            throw NetworkException.Interrupted("Interrupted while calling ${request.uri.host}.", cause)
        }
    }

    private fun toHttpRequest(request: TransportRequest): HttpRequest {
        val builder = HttpRequest.newBuilder(request.uri)
        builder.timeout(timeout)
        builder.setHeader(USER_AGENT_HEADER, userAgent)
        request.headers.forEach { (name, value) -> builder.setHeader(name, value) }
        builder.method(request.method.name, bodyPublisherOf(request))
        return builder.build()
    }

    private fun bodyPublisherOf(request: TransportRequest): HttpRequest.BodyPublisher {
        val body = request.body ?: return HttpRequest.BodyPublishers.noBody()
        return HttpRequest.BodyPublishers.ofByteArray(body)
    }

    private fun readResponse(
        request: TransportRequest,
        response: HttpResponse<InputStream>,
        body: InputStream
    ): TransportResponse {
        val headers = response.headers().map()
        val declaredLength = response.headers().firstValueAsLong(CONTENT_LENGTH_HEADER)
        if (request.method != HttpMethod.HEAD && declaredLength.isPresent) {
            checkDeclaredLength(request, declaredLength.asLong)
        }

        val bytes = readBounded(request, body)
        return TransportResponse(request.uri, response.statusCode(), headers, bytes, clock.instant())
    }

    private fun checkDeclaredLength(request: TransportRequest, declaredLength: Long) {
        if (declaredLength <= request.maxBytes) return
        throw tooLarge(request)
    }

    private fun readBounded(request: TransportRequest, body: InputStream): ByteArray {
        val watchdog = closeOnTimeout(body)
        val bytes = try {
            body.readNBytes(request.maxBytes + 1)
        } catch (cause: IOException) {
            throw NetworkException.Unavailable("The response of ${request.uri.host} was cut off: $cause", cause)
        } finally {
            watchdog.complete(Unit)
        }

        if (bytes.size > request.maxBytes) throw tooLarge(request)
        return bytes
    }

    private fun closeOnTimeout(body: InputStream): CompletableFuture<Unit> {
        val watchdog = CompletableFuture<Unit>()
        watchdog.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
        watchdog.whenComplete { _, timedOut ->
            if (timedOut != null) body.close()
        }
        return watchdog
    }

    private fun tooLarge(request: TransportRequest): NetworkException.TooLarge =
        NetworkException.TooLarge("The response of ${request.uri.host} exceeds ${request.maxBytes} bytes.")
}
