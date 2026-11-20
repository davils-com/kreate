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
package com.davils.kreate.standard.provider.w3c

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.map
import com.davils.kreate.standard.fetch.FetchRequest
import com.davils.kreate.standard.model.ErrataAddress
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.provider.JsonRecords.number
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.document.Fingerprints
import com.davils.kreate.standard.provider.github.GitHubRequests
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val MAX_PAGE_BYTES: Int = 4 * 1024 * 1024

private const val MAX_ANCHORS: Int = 10_000

private const val MAX_ANCHOR_LENGTH: Int = 200

private const val PAGE_SIZE: Int = 100

private const val MAX_PAGES: Int = 10

private const val MAX_SEARCH_BYTES: Int = 16 * 1024 * 1024

private const val CLOSED: String = "closed"

private const val NOT_PLANNED: String = "not_planned"

private val REDIRECT_HOSTS: Set<String> = setOf("w3.org", "www.w3.org", "github.com", "www.github.com")

private val ANCHOR: Regex = Regex(
    """<(?:a\b[^>]*?\sname|[a-zA-Z][a-zA-Z0-9]*\b[^>]*?\sid)\s*=\s*["']([^"'<>\s]{1,$MAX_ANCHOR_LENGTH})["']"""
)

internal class W3cErrata(private val context: ProviderContext) {
    private val requests: GitHubRequests = GitHubRequests(context)

    fun read(address: ErrataAddress): Outcome<ErrataRecord, LookupFailure> = when (address) {
        is ErrataAddress.Page -> pageOf(address)
        is ErrataAddress.Issues -> issuesOf(address)
    }

    private fun pageOf(address: ErrataAddress.Page): Outcome<ErrataRecord, LookupFailure> {
        val uri = context.endpoints.resolve(context.endpoints.w3c, address.path)
        val fetched = context.fetch(FetchRequest(uri, MAX_PAGE_BYTES, redirectHosts = REDIRECT_HOSTS))
        return fetched.map { document ->
            val matches = ANCHOR.findAll(document.text())
            val anchors = matches.map { match -> match.groupValues[1] }
            val distinct = anchors.distinct().take(MAX_ANCHORS)
            val errata = distinct.associateWith { ErratumState.VERIFIED }
            ErrataRecord(errata, Fingerprints.fingerprintOf(document.bytes()))
        }
    }

    private fun issuesOf(address: ErrataAddress.Issues): Outcome<ErrataRecord, LookupFailure> {
        val query = URLEncoder.encode(address.query, StandardCharsets.UTF_8)
        val errata = mutableMapOf<String, ErratumState>()
        for (page in 1..MAX_PAGES) {
            val path = "search/issues?q=$query&per_page=$PAGE_SIZE&page=$page"
            val answer = requests.jsonAt(path, MAX_SEARCH_BYTES)
            val listed = when (answer) {
                is Outcome.Success -> answer.value
                is Outcome.Failure -> return Outcome.Failure(answer.failure)
            }
            val issues = issuesIn(listed)
            errata += issues.mapNotNull { issue -> entryOf(issue) }
            val total = (listed as? JsonObject)?.number("total_count") ?: 0L
            val isLast = issues.size < PAGE_SIZE || page.toLong() * PAGE_SIZE >= total
            if (isLast) return Outcome.Success(ErrataRecord(errata.toMap()))
        }
        return Outcome.Success(ErrataRecord(errata.toMap()))
    }

    private fun issuesIn(listed: JsonElement): List<JsonObject> {
        val items = (listed as? JsonObject)?.get("items") as? JsonArray ?: return emptyList()
        return items.filterIsInstance<JsonObject>()
    }

    private fun entryOf(issue: JsonObject): Pair<String, ErratumState>? {
        val number = issue.number("number") ?: return null
        return "#$number" to stateOf(issue)
    }

    private fun stateOf(issue: JsonObject): ErratumState {
        if (issue.text("state") != CLOSED) return ErratumState.REPORTED
        if (issue.text("state_reason") == NOT_PLANNED) return ErratumState.REJECTED
        return ErratumState.VERIFIED
    }
}
