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

import java.net.ProxySelector
import java.net.http.HttpClient
import java.time.Clock

internal object HttpTransports {
    fun create(
        settings: NetworkSettings,
        clock: Clock,
        sleeper: Sleeper = Sleeper.SYSTEM,
        policy: RetryPolicy = RetryPolicy()
    ): HttpTransport {
        if (settings.offline) return HttpTransport { request -> refuseOffline(request) }

        val client = buildClient(settings)
        val exchange = JavaHttpTransport(client, settings.userAgent, settings.timeout, clock)
        val redirecting = RedirectingTransport(exchange)
        return RetryingTransport(redirecting, policy, sleeper)
    }

    private fun refuseOffline(request: TransportRequest): Nothing =
        throw NetworkException.Offline(
            "Network access is disabled (offline mode), so ${request.uri.host} cannot be contacted."
        )

    private fun buildClient(settings: NetworkSettings): HttpClient {
        val builder = HttpClient.newBuilder()
        builder.followRedirects(HttpClient.Redirect.NEVER)
        builder.connectTimeout(settings.timeout)
        builder.proxy(EnvironmentProxySelector(settings.proxy, ProxySelector.getDefault()))
        settings.caCertificates?.let { pemFile -> builder.sslContext(ExtraCertificateAuthority.sslContext(pemFile)) }
        return builder.build()
    }
}
