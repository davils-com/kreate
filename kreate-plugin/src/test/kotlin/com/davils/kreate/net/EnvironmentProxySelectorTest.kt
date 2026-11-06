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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class EnvironmentProxySelectorTest : FunSpec({
    val target = URI("https://example.com/")

    val fallbackProxy = Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("fallback.test", 1))

    class RecordingSelector : ProxySelector() {
        val failures = mutableListOf<URI>()

        override fun select(uri: URI): List<Proxy> = listOf(fallbackProxy)

        override fun connectFailed(uri: URI, address: SocketAddress, failure: IOException) {
            failures.add(uri)
        }
    }

    test("uses the configured proxy") {
        val selector = EnvironmentProxySelector(ProxySettings("http://proxy.test:3128"), RecordingSelector())

        val proxy = selector.select(target).single()

        proxy.type() shouldBe Proxy.Type.HTTP
        proxy.address() shouldBe InetSocketAddress.createUnresolved("proxy.test", 3128)
    }

    test("goes direct for a NO_PROXY match") {
        val settings = ProxySettings("http://proxy.test:3128", "example.com")
        val selector = EnvironmentProxySelector(settings, RecordingSelector())

        selector.select(target) shouldBe listOf(Proxy.NO_PROXY)
    }

    test("never sends loopback traffic through a proxy") {
        val selector = EnvironmentProxySelector(ProxySettings("http://proxy.test:3128"), RecordingSelector())

        listOf("http://127.0.0.1:1/", "http://localhost/", "https://[::1]/").forEach { uri ->
            selector.select(URI(uri)) shouldBe listOf(Proxy.NO_PROXY)
        }
    }

    test("falls back to the default selector without a configured proxy") {
        val selector = EnvironmentProxySelector(ProxySettings(), RecordingSelector())

        selector.select(target) shouldBe listOf(fallbackProxy)
    }

    test("treats a blank proxy value as unset") {
        val selector = EnvironmentProxySelector(ProxySettings(httpsProxy = "  "), RecordingSelector())

        selector.select(target) shouldBe listOf(fallbackProxy)
    }

    test("goes direct when there is no default selector") {
        val selector = EnvironmentProxySelector(ProxySettings(), fallback = null)

        selector.select(target) shouldBe listOf(Proxy.NO_PROXY)
    }

    test("reports a failed connection to the default selector") {
        val fallback = RecordingSelector()
        val selector = EnvironmentProxySelector(ProxySettings(), fallback)

        selector.connectFailed(target, InetSocketAddress(0), IOException("refused"))
        EnvironmentProxySelector(ProxySettings(), null).connectFailed(target, InetSocketAddress(0), IOException("x"))

        fallback.failures shouldHaveSize 1
    }

    test("handles an address without a host") {
        val selector = EnvironmentProxySelector(ProxySettings(), RecordingSelector())

        selector.select(URI("/relative")) shouldBe listOf(fallbackProxy)
    }
})
