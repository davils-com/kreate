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

import java.net.URI
import java.time.Duration
import java.time.Instant

private const val HTTP_OK: Int = 200

private const val HTTP_MULTIPLE_CHOICES: Int = 300

private const val HTTP_NOT_FOUND: Int = 404

internal class TransportResponse(
    val uri: URI,
    val statusCode: Int,
    headers: Map<String, List<String>>,
    val body: ByteArray,
    val receivedAt: Instant
) {
    private val lowerCaseHeaders: Map<String, List<String>> = headers.mapKeys { (name, _) -> name.lowercase() }

    val isSuccess: Boolean
        get() = statusCode in HTTP_OK until HTTP_MULTIPLE_CHOICES

    val isNotFound: Boolean
        get() = statusCode == HTTP_NOT_FOUND

    val text: String
        get() = body.toString(Charsets.UTF_8)

    val retryAfter: Duration?
        get() {
            val seconds = header("retry-after")?.toLongOrNull() ?: return null
            if (seconds < 0) return null
            return Duration.ofSeconds(seconds)
        }

    fun header(name: String): String? {
        val values = lowerCaseHeaders[name.lowercase()]
        return values?.firstOrNull()
    }

    fun redirectTarget(): URI? {
        val location = header("location") ?: return null
        return try {
            uri.resolve(location)
        } catch (cause: IllegalArgumentException) {
            throw NetworkException.Refused("The redirect from ${uri.host} has an invalid target.", cause)
        }
    }
}
