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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val SECURE_SCHEME: String = "https"

private const val PLAIN_SCHEME: String = "http"

private val W3C_HOSTS: Set<String> = setOf("w3.org", "www.w3.org")

private val GITHUB_HOSTS: Set<String> = setOf("github.com", "www.github.com")

private const val ISSUES_SEGMENT: String = "issues"

private const val LABELS_SEGMENT: String = "labels"

private const val QUERY_PARAMETER: String = "q="

private const val ISSUES_PATH_SIZE: Int = 3

private const val LABEL_PATH_SIZE: Int = 4

private val DROPPED_TERM: Regex = Regex("^(?:(?:is|state):(?:open|closed|issue|pr)|(?:repo|org|user):.*)$")

private val WHITESPACE: Regex = Regex("\\s+")

internal sealed class ErrataAddress {
    class Page(val path: String) : ErrataAddress()

    class Issues(val repository: String, val terms: String) : ErrataAddress() {
        val query: String
            get() {
                val all = listOf("repo:$repository", "is:issue", terms)
                return all.filter { term -> term.isNotEmpty() }.joinToString(" ")
            }
    }

    companion object {
        fun parse(url: String, upgradesPlainHttp: Boolean = false): ErrataAddress? {
            val uri = uriOf(url) ?: return null
            val scheme = uri.scheme?.lowercase()
            val isUpgraded = upgradesPlainHttp && scheme == PLAIN_SCHEME
            val isSecure = scheme == SECURE_SCHEME || isUpgraded
            if (!isSecure || uri.rawUserInfo != null || uri.port != -1) return null
            val host = uri.host?.lowercase() ?: return null
            if (host in W3C_HOSTS) return pageOf(uri)
            if (host in GITHUB_HOSTS) return issuesOf(uri)
            return null
        }

        private fun uriOf(url: String): URI? = try {
            URI(url.trim())
        } catch (_: URISyntaxException) {
            null
        }

        private fun pageOf(uri: URI): Page {
            val path = uri.rawPath.orEmpty().ifEmpty { "/" }
            val query = uri.rawQuery?.let { text -> "?$text" }.orEmpty()
            return Page(path + query)
        }

        private fun issuesOf(uri: URI): Issues? {
            val path = uri.path.orEmpty().trim('/')
            val segments = path.split('/')
            val owned = segments.take(2).joinToString("/")
            val repository = owned.lowercase()
            if (segments.size < ISSUES_PATH_SIZE) return null
            if (!StandardIdGrammar.isValid(StandardKind.GITHUB, repository)) return null
            val isIssues = segments.size == ISSUES_PATH_SIZE && segments[2] == ISSUES_SEGMENT
            if (isIssues) return Issues(repository, termsOf(uri.rawQuery))
            val isLabel = segments.size == LABEL_PATH_SIZE && segments[2] == LABELS_SEGMENT
            if (!isLabel) return null
            return Issues(repository, "label:\"${segments.last()}\"")
        }

        private fun termsOf(rawQuery: String?): String {
            val parameters = rawQuery.orEmpty().split('&')
            val encoded = parameters.firstOrNull { parameter -> parameter.startsWith(QUERY_PARAMETER) } ?: return ""
            val decoded = URLDecoder.decode(encoded.removePrefix(QUERY_PARAMETER), StandardCharsets.UTF_8)
            val terms = decoded.trim().split(WHITESPACE)
            val kept = terms.filterNot { term -> term.isEmpty() || DROPPED_TERM.matches(term.lowercase()) }
            return kept.joinToString(" ")
        }
    }
}
