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
package com.davils.kreate.assurance.report

import java.security.MessageDigest
import java.util.Base64

private const val STYLE_RESOURCE: String = "/assurance/html/dashboard.css"

private const val SCRIPT_RESOURCE: String = "/assurance/html/dashboard.js"

private const val HASH_ALGORITHM: String = "SHA-256"

internal object HtmlDashboard {
    private val style: String by lazy { resourceOf(STYLE_RESOURCE) }

    private val script: String by lazy { resourceOf(SCRIPT_RESOURCE) }

    fun page(title: String, body: String): String {
        val policy = "default-src 'none'; style-src '${hashOf(style)}'; script-src '${hashOf(script)}'; " +
            "img-src data:; base-uri 'none'; form-action 'none'"
        return listOf(
            "<!DOCTYPE html>",
            "<html lang=\"en\">",
            "<head>",
            "<meta charset=\"utf-8\">",
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">",
            "<meta http-equiv=\"Content-Security-Policy\" content=\"$policy\">",
            "<meta name=\"referrer\" content=\"no-referrer\">",
            "<title>${ReportEscaping.xml(title)}</title>",
            "<style>$style</style>",
            "</head>",
            "<body><main>",
            body,
            "</main><script>$script</script></body>",
            "</html>"
        ).joinToString("\n", postfix = "\n")
    }

    fun hashOf(content: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(content.toByteArray())
        return "sha256-" + Base64.getEncoder().encodeToString(digest)
    }

    private fun resourceOf(name: String): String {
        val stream = HtmlDashboard::class.java.getResourceAsStream(name) ?: error("The resource $name is missing.")
        return stream.use { input -> input.readBytes().decodeToString() }
    }
}
