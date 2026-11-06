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

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import java.net.ProxySelector
import java.time.Clock
import java.net.http.HttpClient as JdkHttpClient

internal object HttpTransports {
    fun create(
        settings: NetworkSettings,
        clock: Clock,
        sleeper: Sleeper = Sleeper.SYSTEM,
        policy: RetryPolicy = RetryPolicy()
    ): HttpTransport {
        if (settings.offline) return HttpTransport { request -> refuseOffline(request) }

        val exchange = KtorHttpTransport(buildClient(settings), settings.userAgent, settings.timeout, clock)
        val redirecting = RedirectingTransport(exchange)
        return RetryingTransport(redirecting, policy, sleeper)
    }

    private fun refuseOffline(request: TransportRequest): Nothing =
        throw NetworkException.Offline(
            "Network access is disabled (offline mode), so ${request.uri.host} cannot be contacted."
        )

    fun buildClient(settings: NetworkSettings): HttpClient {
        val proxySelector = EnvironmentProxySelector(settings.proxy, ProxySelector.getDefault())
        val trustedAuthorities = settings.caCertificates?.let(ExtraCertificateAuthority::sslContext)

        return HttpClient(Java) {
            followRedirects = false
            useDefaultTransformers = false

            engine {
                config {
                    followRedirects(JdkHttpClient.Redirect.NEVER)
                    connectTimeout(settings.timeout)
                    proxy(proxySelector)
                    trustedAuthorities?.let(::sslContext)
                }
            }
        }
    }
}
