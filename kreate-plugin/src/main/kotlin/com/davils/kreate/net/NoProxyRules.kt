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

import java.net.URI

private const val ANY_HOST: String = "*"

private const val HTTPS_DEFAULT_PORT: Int = 443

private val PORT_SUFFIX: Regex = Regex(""":(\d+)$""")

internal class NoProxyRules(value: String?) {
    private val entries: List<String> = parseEntries(value.orEmpty())

    private val matchesEverything: Boolean = ANY_HOST in entries

    private val rules: List<NoProxyRule> = entries.map(::parseRule)

    fun matches(uri: URI): Boolean {
        if (matchesEverything) return true

        val host = uri.host ?: return false
        val port = uri.port.takeIf { declared -> declared >= 0 } ?: HTTPS_DEFAULT_PORT
        val normalizedHost = withoutBrackets(host.lowercase())
        return rules.any { rule -> rule.matches(normalizedHost, port) }
    }

    private fun parseEntries(value: String): List<String> {
        val entries = value.split(',').map { entry -> entry.trim() }
        return entries.filter { entry -> entry.isNotEmpty() }.map { entry -> entry.lowercase() }
    }

    private fun parseRule(entry: String): NoProxyRule {
        val portMatch = portMatchOf(entry)
        val portText = portMatch?.groupValues?.lastOrNull()
        val hostPart = entry.removeSuffix(portMatch?.value.orEmpty())
        val host = withoutBrackets(hostPart.removePrefix("."))
        return NoProxyRule(host, portText?.toIntOrNull())
    }

    private fun portMatchOf(entry: String): MatchResult? {
        val isBareIpv6 = entry.count { character -> character == ':' } > 1 && !entry.startsWith('[')
        if (isBareIpv6) return null
        return PORT_SUFFIX.find(entry)
    }

    private fun withoutBrackets(host: String): String = host.removeSurrounding("[", "]")
}
