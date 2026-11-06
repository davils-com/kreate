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

private const val MAX_REDIRECTS: Int = 5

private const val HTTPS_DEFAULT_PORT: Int = 443

private const val HTTP_DEFAULT_PORT: Int = 80

private val REDIRECT_STATUS_CODES: Set<Int> = setOf(301, 302, 303, 307, 308)

private val CROSS_ORIGIN_HEADERS: Set<String> = setOf("accept")

internal class RedirectingTransport(private val delegate: HttpTransport) : HttpTransport {
    override fun close() {
        delegate.close()
    }

    override fun send(request: TransportRequest): TransportResponse {
        val guard = UriGuard(request.allowedHosts)
        guard.check(request.uri)
        return follow(request, guard, followed = 0)
    }

    private tailrec fun follow(request: TransportRequest, guard: UriGuard, followed: Int): TransportResponse {
        val response = delegate.send(request)
        val target = redirectTargetOf(request, response) ?: return response

        if (followed >= MAX_REDIRECTS) {
            throw NetworkException.Refused("${request.uri.host} redirected more than $MAX_REDIRECTS times.")
        }

        guard.checkHop(request.uri, target)
        val keptHeaders = headersFor(request, target)
        return follow(request.redirectedTo(target, keptHeaders), guard, followed + 1)
    }

    private fun redirectTargetOf(request: TransportRequest, response: TransportResponse): URI? {
        if (request.method == HttpMethod.POST) return null
        if (response.statusCode !in REDIRECT_STATUS_CODES) return null
        return response.redirectTarget()
    }

    private fun headersFor(request: TransportRequest, target: URI): Map<String, String> {
        if (isSameOrigin(request.uri, target)) return request.headers
        return request.headers.filterKeys { name -> name.lowercase() in CROSS_ORIGIN_HEADERS }
    }

    private fun isSameOrigin(first: URI, second: URI): Boolean {
        val hasSameScheme = first.scheme.equals(second.scheme, ignoreCase = true)
        val hasSameHost = first.host.equals(second.host, ignoreCase = true)
        return hasSameScheme && hasSameHost && effectivePort(first) == effectivePort(second)
    }

    private fun effectivePort(uri: URI): Int {
        if (uri.port >= 0) return uri.port
        if (uri.scheme.equals("https", ignoreCase = true)) return HTTPS_DEFAULT_PORT
        return HTTP_DEFAULT_PORT
    }
}
