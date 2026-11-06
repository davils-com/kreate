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
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

internal class EnvironmentProxySelector(
    settings: ProxySettings,
    private val fallback: ProxySelector?
) : ProxySelector() {
    private val proxy: Proxy? = settings.httpsProxy
        ?.takeIf { value -> value.isNotBlank() }
        ?.let { value -> Proxy(Proxy.Type.HTTP, ProxyAddress.parse(value)) }

    private val noProxy: NoProxyRules = NoProxyRules(settings.noProxy)

    override fun select(uri: URI): List<Proxy> {
        if (isLoopback(uri) || noProxy.matches(uri)) return DIRECT
        if (proxy != null) return listOf(proxy)
        return fallback?.select(uri) ?: DIRECT
    }

    override fun connectFailed(uri: URI, address: SocketAddress, failure: IOException) {
        fallback?.connectFailed(uri, address, failure)
    }

    private fun isLoopback(uri: URI): Boolean {
        val host = uri.host ?: return false
        return HostClassifier.classify(host) == HostKind.LOOPBACK
    }

    private companion object {
        private val DIRECT: List<Proxy> = listOf(Proxy.NO_PROXY)
    }
}
