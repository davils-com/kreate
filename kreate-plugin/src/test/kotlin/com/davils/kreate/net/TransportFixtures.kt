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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal val FIXED_INSTANT: Instant = Instant.parse("2026-11-06T12:00:00Z")

internal val FIXED_CLOCK: Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

internal fun get(uri: String, headers: Map<String, String> = emptyMap()): TransportRequest =
    TransportRequest(HttpMethod.GET, URI(uri), headers)

internal fun responseOf(
    status: Int,
    uri: String = "https://example.com/",
    headers: Map<String, String> = emptyMap(),
    body: ByteArray = ByteArray(0)
): TransportResponse {
    val multiValueHeaders = headers.mapValues { (_, value) -> listOf(value) }
    return TransportResponse(URI(uri), status, multiValueHeaders, body, FIXED_INSTANT)
}
