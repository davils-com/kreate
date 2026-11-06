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

internal class TransportRequest(
    val method: HttpMethod,
    val uri: URI,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val maxBytes: Int = DEFAULT_MAX_BYTES,
    val allowedHosts: Set<String> = emptySet()
) {
    init {
        require(maxBytes in 0 until Int.MAX_VALUE) { "The response limit must leave room to detect an overrun." }
    }

    fun redirectedTo(target: URI, keptHeaders: Map<String, String>): TransportRequest =
        TransportRequest(method, target, keptHeaders, body, maxBytes, allowedHosts)

    companion object {
        const val DEFAULT_MAX_BYTES: Int = 64 * 1024 * 1024
    }
}
