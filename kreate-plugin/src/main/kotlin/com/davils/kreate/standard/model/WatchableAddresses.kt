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
package com.davils.kreate.standard.model

import java.net.URI
import java.net.URISyntaxException

private const val SECURE_SCHEME: String = "https"

private const val PLAIN_SCHEME: String = "http"

private val LOOPBACK_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "[::1]", "::1")

internal object WatchableAddresses {
    fun isWatchable(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (uri.host == null || uri.userInfo != null) return false
        if (scheme == SECURE_SCHEME) return true
        return scheme == PLAIN_SCHEME && uri.host in LOOPBACK_HOSTS
    }
}
