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
package com.davils.kreate.standard.provider

import java.net.URI

internal class ServiceEndpoints(private val urls: Map<String, String>) {
    val rfcEditor: URI
        get() = baseOf(RFC_EDITOR)

    val datatracker: URI
        get() = baseOf(DATATRACKER)

    val w3cApi: URI
        get() = baseOf(W3C_API)

    val w3c: URI
        get() = baseOf(W3C)

    val githubApi: URI
        get() = baseOf(GITHUB_API)

    val unicode: URI
        get() = baseOf(UNICODE)

    val isoOpenData: URI
        get() = baseOf(ISO_OPEN_DATA)

    fun resolve(base: URI, path: String): URI {
        val root = base.toString().trimEnd('/')
        return URI("$root/${path.trimStart('/')}")
    }

    private fun baseOf(service: String): URI {
        val url = urls[service] ?: DEFAULTS.getValue(service)
        return URI(url)
    }

    companion object {
        const val RFC_EDITOR: String = "rfcEditorUrl"

        const val DATATRACKER: String = "datatrackerUrl"

        const val W3C_API: String = "w3cApiUrl"

        const val W3C: String = "w3cUrl"

        const val GITHUB_API: String = "githubApiUrl"

        const val UNICODE: String = "unicodeUrl"

        const val ISO_OPEN_DATA: String = "isoOpenDataUrl"

        val DEFAULTS: Map<String, String> = mapOf(
            RFC_EDITOR to "https://www.rfc-editor.org",
            DATATRACKER to "https://datatracker.ietf.org",
            W3C_API to "https://api.w3.org",
            W3C to "https://www.w3.org",
            GITHUB_API to "https://api.github.com",
            UNICODE to "https://www.unicode.org",
            ISO_OPEN_DATA to "https://isopublicstorageprod.blob.core.windows.net/opendata/_latest"
        )
    }
}
