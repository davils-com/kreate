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

import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException

private const val HTTP_SCHEME: String = "http"

private const val SCHEME_SEPARATOR: String = "://"

private const val DEFAULT_PROXY_PORT: Int = 80

internal object ProxyAddress {
    fun parse(value: String): InetSocketAddress {
        val uri = parseUri(value)
        val host = uri.host ?: invalid("The proxy address names no host.")

        if (uri.rawUserInfo != null) invalid("Proxy credentials in the proxy address are not supported.")
        if (!HTTP_SCHEME.equals(uri.scheme, ignoreCase = true)) {
            invalid("Only an HTTP proxy is supported, not '${uri.scheme}'.")
        }

        val port = uri.port.takeIf { declared -> declared >= 0 } ?: DEFAULT_PROXY_PORT
        return InetSocketAddress.createUnresolved(host, port)
    }

    private fun parseUri(value: String): URI {
        val trimmed = value.trim()
        val withScheme = trimmed.takeIf { SCHEME_SEPARATOR in it } ?: "$HTTP_SCHEME$SCHEME_SEPARATOR$trimmed"

        return try {
            URI(withScheme)
        } catch (cause: URISyntaxException) {
            throw NetworkException.Misconfigured("The proxy address is not a valid URI.", cause)
        }
    }

    private fun invalid(message: String): Nothing = throw NetworkException.Misconfigured(message)
}
