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

private const val HTTPS_SCHEME: String = "https"

private const val HTTP_SCHEME: String = "http"

private val LOCAL_HOST_KINDS: Set<HostKind> = setOf(HostKind.LOOPBACK, HostKind.PRIVATE)

internal class UriGuard(allowedHosts: Set<String>) {
    private val allowedHosts: Set<String> = allowedHosts.mapTo(HashSet()) { host -> host.lowercase() }

    fun check(uri: URI) {
        val host = uri.host ?: refuse("The address has no valid host.")
        val kind = HostClassifier.classify(host)

        if (uri.rawUserInfo != null) refuse("The address of $host embeds credentials.")
        if (kind == HostKind.AMBIGUOUS) refuse("The host $host is an ambiguous numeric address.")
        checkScheme(uri, host, kind)
        checkAllowed(host)
    }

    fun checkHop(origin: URI, target: URI) {
        check(target)

        val isDowngrade = origin.isScheme(HTTPS_SCHEME) && target.isScheme(HTTP_SCHEME)
        if (isDowngrade) refuse("A redirect may not leave HTTPS, but ${origin.host} sent one to ${target.host}.")

        val entersLocalNetwork = kindOf(origin) !in LOCAL_HOST_KINDS && kindOf(target) in LOCAL_HOST_KINDS
        if (entersLocalNetwork) refuse("A redirect from ${origin.host} may not reach the local host ${target.host}.")
    }

    private fun checkScheme(uri: URI, host: String, kind: HostKind) {
        if (uri.isScheme(HTTPS_SCHEME)) return

        val isPlainHttp = uri.isScheme(HTTP_SCHEME)
        if (isPlainHttp && kind == HostKind.LOOPBACK) return

        refuse("The scheme '${uri.scheme}' is not allowed for $host, only HTTPS or loopback HTTP.")
    }

    private fun checkAllowed(host: String) {
        if (allowedHosts.isEmpty()) return

        val isAllowed = host.lowercase() in allowedHosts
        if (!isAllowed) refuse("The host $host is not one of the allowed hosts.")
    }

    private fun kindOf(uri: URI): HostKind {
        val host = uri.host ?: refuse("The address has no valid host.")
        return HostClassifier.classify(host)
    }

    private fun URI.isScheme(expected: String): Boolean = expected.equals(scheme, ignoreCase = true)

    private fun refuse(message: String): Nothing = throw NetworkException.Refused(message)
}
