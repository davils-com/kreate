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

import com.davils.kore.core.collection.duplicatesBy
import com.davils.kore.core.text.SafeText
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.PatternSyntaxException

private const val SECURE_SCHEME: String = "https"

private const val PLAIN_SCHEME: String = "http"

private val LOOPBACK_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "[::1]", "::1")

private val MANUAL_KINDS: Set<StandardKind> = setOf(StandardKind.DIN, StandardKind.DOCUMENT)

internal object DeclarationValidator {
    fun problemsOf(declarations: List<StandardDeclaration>, defaultReviewEverySeconds: Long?): List<String> {
        val duplicated = declarations.duplicatesBy { declaration -> declaration.id }
        val duplicates = duplicated.map { id -> "$id is declared more than once." }
        val defaultProblem = defaultReviewEverySeconds
            ?.takeIf { seconds -> seconds <= 0 }
            ?.let { "standards { reviewEvery } must be a positive duration." }
        val each = declarations.flatMap { declaration -> problemsOf(declaration, defaultReviewEverySeconds) }
        return duplicates + listOfNotNull(defaultProblem) + each
    }

    private fun problemsOf(declaration: StandardDeclaration, defaultReviewEverySeconds: Long?): List<String> {
        val id = declaration.id
        val interval = declaration.reviewEverySeconds ?: defaultReviewEverySeconds
        val isManual = id.kind in MANUAL_KINDS
        val unchecked = "$id needs reviewEvery, because nothing can check it for changes."
        val negative = "$id needs a positive reviewEvery."
        return listOfNotNull(
            unchecked.takeIf { isManual && interval == null },
            negative.takeIf { isNotPositive(declaration.reviewEverySeconds) },
            tagsProblem(declaration),
            urlProblem(declaration, isManual)
        )
    }

    private fun isNotPositive(seconds: Long?): Boolean = seconds != null && seconds <= 0

    private fun tagsProblem(declaration: StandardDeclaration): String? {
        val tags = declaration.tags ?: return null
        if (declaration.id.kind != StandardKind.GITHUB) return "${declaration.id} takes no tags; only github() does."
        if (compiles(tags)) return null
        return "${declaration.id} has tags ${SafeText.quoted(tags)}, which are no regular expression."
    }

    private fun compiles(pattern: String): Boolean = try {
        Regex(pattern).pattern == pattern
    } catch (_: PatternSyntaxException) {
        false
    }

    private fun urlProblem(declaration: StandardDeclaration, isManual: Boolean): String? {
        val url = declaration.url ?: return null
        if (!isManual) return "${declaration.id} takes no url; only din() and document() do."
        if (isWatchable(url)) return null
        return "${declaration.id} has the url ${SafeText.quoted(url)}, which is neither HTTPS nor a loopback address."
    }

    private fun isWatchable(url: String): Boolean {
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
