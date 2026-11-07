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
package com.davils.kreate.provenance.depsdev

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.net.HttpMethod
import com.davils.kreate.net.HttpTransport
import com.davils.kreate.net.NetworkException
import com.davils.kreate.net.TransportRequest
import com.davils.kreate.net.TransportResponse
import java.net.URI
import java.net.URLEncoder

private const val MAX_ANSWER_BYTES: Int = 8 * 1024 * 1024

internal class DepsDevClient(
    private val transport: HttpTransport,
    baseUrl: String,
    private val cache: DepsDevCache
) {
    private val base: String = baseUrl.trimEnd('/')

    private val host: Set<String> = setOfNotNull(URI(base).host)

    fun historyOf(module: ModuleCoordinate): PackageHistory? {
        val path = "/v3/systems/maven/packages/${encoded(module.module)}"
        val cached = cache.read(base + path)?.let(DepsDevParser::historyOf)
        if (cached != null && cached.knows(module.version)) return cached

        val text = fetch(path) ?: return null
        val history = DepsDevParser.historyOf(text)
        cache.write(base + path, text)
        return history
    }

    fun sourceProjectsOf(module: ModuleCoordinate): List<String> {
        val path = "/v3/systems/maven/packages/${encoded(module.module)}/versions/${encoded(module.version)}"
        val text = cachedOrFetched(path) ?: return emptyList()
        return DepsDevParser.sourceProjectsOf(text)
    }

    fun scorecardOf(project: String): Double? {
        val text = cachedOrFetched("/v3/projects/${encoded(project)}") ?: return null
        return DepsDevParser.scorecardOf(text)
    }

    private fun cachedOrFetched(path: String): String? {
        val cached = cache.read(base + path)
        if (cached != null) return cached
        val text = fetch(path) ?: return null
        cache.write(base + path, text)
        return text
    }

    private fun fetch(path: String): String? {
        val request = TransportRequest(
            method = HttpMethod.GET,
            uri = URI(base + path),
            maxBytes = MAX_ANSWER_BYTES,
            allowedHosts = host
        )
        val response = send(request)
        if (response.isNotFound) return null
        if (!response.isSuccess) {
            throw DepsDevUnavailableException("deps.dev answered $path with status ${response.statusCode}.")
        }
        return response.text
    }

    private fun send(request: TransportRequest): TransportResponse = try {
        transport.send(request)
    } catch (cause: NetworkException) {
        throw DepsDevUnavailableException("deps.dev could not be reached: ${cause.message}", cause)
    }

    private fun encoded(segment: String): String {
        val encoded = URLEncoder.encode(segment, Charsets.UTF_8)
        return encoded.replace("+", "%20")
    }
}
