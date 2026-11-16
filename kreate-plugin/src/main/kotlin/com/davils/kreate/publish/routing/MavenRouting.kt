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

package com.davils.kreate.publish.routing

import com.davils.kreate.settings.local.SNAPSHOT_SUFFIX
import java.net.URI
import java.net.URISyntaxException

private const val INSECURE_SCHEME: String = "http"

private val SECURE_SCHEMES: Set<String> = setOf("https", "file")

internal fun repositoryKindOf(version: String): RepositoryKind {
    if (version.endsWith(SNAPSHOT_SUFFIX)) return RepositoryKind.SNAPSHOT
    return RepositoryKind.RELEASE
}

internal fun addressFor(kind: RepositoryKind, releases: String?, snapshots: String?): String? = when (kind) {
    RepositoryKind.RELEASE -> releases
    RepositoryKind.SNAPSHOT -> snapshots
}

internal fun addressProblemOf(address: String, allowInsecureProtocol: Boolean): AddressProblem? {
    val uri = parsedOrNull(address) ?: return AddressProblem.MALFORMED
    if (uri.userInfo != null) return AddressProblem.EMBEDDED_CREDENTIALS

    val scheme = uri.scheme?.lowercase()
    if (scheme in SECURE_SCHEMES) return null
    if (scheme != INSECURE_SCHEME) return AddressProblem.UNSUPPORTED_SCHEME
    if (allowInsecureProtocol) return null
    return AddressProblem.INSECURE
}

internal fun loginStateOf(username: String?, password: String?): LoginState {
    val isUsernameSet = username != null
    val isPasswordSet = password != null
    if (isUsernameSet && isPasswordSet) return LoginState.COMPLETE
    if (isUsernameSet || isPasswordSet) return LoginState.PARTIAL
    return LoginState.ANONYMOUS
}

private fun parsedOrNull(address: String): URI? = try {
    URI(address)
} catch (_: URISyntaxException) {
    null
}
