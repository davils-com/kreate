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
package com.davils.kreate.inspection.scan

import java.net.URI
import java.net.URISyntaxException

private val TEXT_RULES: Map<String, Regex> = mapOf(
    "CRYPTO_MINING" to Regex("""stratum\+(?:tcp|ssl)://|coinhive|xmrig|cryptonight""", RegexOption.IGNORE_CASE),
    "BACKDOOR_PATTERN" to Regex("""reverse.?shell|bind.?shell|/dev/tcp/|\bnc\s+-e\b""", RegexOption.IGNORE_CASE),
    "DOWNLOAD_AND_EXECUTE" to Regex(
        """(?:curl|wget|iwr|Invoke-WebRequest)\b[^|\n]{0,200}\|\s*(?:sh|bash|zsh|iex|Invoke-Expression)\b""",
        RegexOption.IGNORE_CASE
    ),
    "ENCODED_POWERSHELL" to Regex(
        """powershell[^\n]{0,40}\s-(?:e|enc|encodedcommand)\s+[A-Za-z0-9+/=]{20,}""",
        RegexOption.IGNORE_CASE
    )
)

private val URL_PATTERN: Regex = Regex("""https?://[^\s'"`<>)\]]{4,}""", RegexOption.IGNORE_CASE)

private const val MAX_SCANNED_CHARACTERS: Int = 16 * 1024

internal object TextPatterns {
    fun scan(text: String, path: String, hits: MutableCollection<Hit>) {
        val bounded = text.take(MAX_SCANNED_CHARACTERS)
        val matching = TEXT_RULES.filterValues { pattern -> pattern.containsMatchIn(bounded) }
        matching.keys.forEach { rule -> hits += Hit(rule, path) }

        val urls = URL_PATTERN.findAll(bounded).toList()
        val hosts = urls.mapNotNull { url -> hostOf(url.value) }
        hosts.forEach { host -> hits += Hit("HARDCODED_URL", path, host) }
    }
}

internal fun hostOf(url: String): String? = try {
    URI(url).host?.lowercase()
} catch (_: URISyntaxException) {
    null
}
