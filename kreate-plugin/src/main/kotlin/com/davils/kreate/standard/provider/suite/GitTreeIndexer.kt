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
package com.davils.kreate.standard.provider.suite

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.flatMap
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.github.GitHubRequests
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val MAX_TREE_BYTES: Int = 64 * 1024 * 1024

private const val BLOB: String = "blob"

private const val TRUE: String = "true"

internal class GitTreeIndexer(private val requests: GitHubRequests) {
    fun indexOf(repository: String, commit: String, root: String?): Outcome<SuiteIndex, LookupFailure> {
        val tree = requests.jsonAt("repos/$repository/git/trees/$commit?recursive=1", MAX_TREE_BYTES)
        return tree.flatMap { listed -> indexOf(repository, commit, root, listed) }
    }

    private fun indexOf(
        repository: String,
        commit: String,
        root: String?,
        listed: JsonElement
    ): Outcome<SuiteIndex, LookupFailure> {
        val json = listed as? JsonObject
            ?: return Outcome.Failure(LookupFailure.Unavailable("The tree of $repository at $commit is no object."))
        if (json["truncated"]?.toString() == TRUE) {
            val reason = "GitHub truncates the tree of $repository at $commit, so its files cannot be compared."
            return Outcome.Failure(LookupFailure.Unavailable(reason))
        }
        val entries = (json["tree"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
        val blobs = entries.filter { entry -> entry.text("type") == BLOB }
        val digests = blobs.mapNotNull { entry -> pairOf(entry) }.toMap()
        return Outcome.Success(SuiteIndex.rooted(digests, root.orEmpty(), isGitBlob = true))
    }

    private fun pairOf(entry: JsonObject): Pair<String, String>? {
        val path = entry.text("path") ?: return null
        val sha = entry.text("sha") ?: return null
        return path to sha
    }
}
