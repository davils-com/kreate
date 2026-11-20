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
package com.davils.kreate.standard.provider.github

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.provider.JsonRecords
import com.davils.kreate.standard.provider.ProviderContext
import kotlinx.serialization.json.JsonElement

private const val SECURE_SCHEME: String = "https"

private const val DEFAULT_MAX_BYTES: Int = 8 * 1024 * 1024

private val BASE_HEADERS: Map<String, String> = mapOf(
    "Accept" to "application/vnd.github+json",
    "X-GitHub-Api-Version" to "2022-11-28"
)

internal class GitHubRequests(private val context: ProviderContext) {
    fun jsonAt(path: String, maxBytes: Int = DEFAULT_MAX_BYTES): Outcome<JsonElement, LookupFailure> {
        val uri = context.endpoints.resolve(context.endpoints.githubApi, path)
        val fetched = context.fetch(FetchRequest(uri, maxBytes, headers()))
        return fetched.flatMap { document ->
            val json = JsonRecords.elementOf(document.text())
            json?.let { found -> Outcome.Success(found) }
                ?: Outcome.Failure(LookupFailure.Unavailable("The answer of GitHub to $path is no JSON."))
        }
    }

    private fun headers(): Map<String, String> {
        val token = context.githubToken ?: return BASE_HEADERS
        if (context.endpoints.githubApi.scheme != SECURE_SCHEME) return BASE_HEADERS
        return BASE_HEADERS + ("Authorization" to "Bearer $token")
    }
}
