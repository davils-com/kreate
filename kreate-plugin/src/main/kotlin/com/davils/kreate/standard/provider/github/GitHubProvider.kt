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
import com.davils.kore.core.version.SemanticVersion
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardKind
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.JsonRecords.text
import com.davils.kreate.standard.provider.ProviderContext
import com.davils.kreate.standard.provider.StandardProvider
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val PAGE: String = "per_page=100"

internal class GitHubProvider(context: ProviderContext) : StandardProvider {
    private val requests: GitHubRequests = GitHubRequests(context)

    override fun accepts(declaration: StandardDeclaration): Boolean = declaration.id.kind == StandardKind.GITHUB

    override fun snapshotOf(declaration: StandardDeclaration): Outcome<StandardSnapshot, LookupFailure> {
        val repository = declaration.id.name
        val pattern = declaration.tags?.let(::Regex)
        val releases = requests.jsonAt("repos/$repository/releases?$PAGE")
        return releases.flatMap { listed ->
            val release = latestRelease(listed, pattern)
            release?.let { found -> Outcome.Success(snapshotOf(repository, found)) } ?: fromTags(repository, pattern)
        }
    }

    private fun fromTags(repository: String, pattern: Regex?): Outcome<StandardSnapshot, LookupFailure> {
        val tags = requests.jsonAt("repos/$repository/tags?$PAGE")
        return tags.flatMap { listed ->
            val names = objectsOf(listed).mapNotNull { tag -> tag.text("name") }
            val matching = names.filter { name -> pattern?.containsMatchIn(name) ?: true }
            val latest = newestOf(matching)
            Outcome.Success(StandardSnapshot(title = repository, revision = latest))
        }
    }

    private fun latestRelease(listed: JsonElement, pattern: Regex?): JsonObject? {
        val releases = objectsOf(listed).filter { release -> isFinal(release) }
        return releases.firstOrNull { release -> pattern?.containsMatchIn(release.text("tag_name").orEmpty()) ?: true }
    }

    private fun snapshotOf(repository: String, release: JsonObject): StandardSnapshot = StandardSnapshot(
        title = repository,
        revision = release.text("tag_name"),
        published = release.text("published_at")?.substringBefore('T')
    )

    private fun isFinal(release: JsonObject): Boolean {
        val isDraft = release["draft"]?.toString() == "true"
        val isPreRelease = release["prerelease"]?.toString() == "true"
        return !isDraft && !isPreRelease
    }

    private fun objectsOf(listed: JsonElement): List<JsonObject> {
        val array = listed as? JsonArray ?: return emptyList()
        return array.filterIsInstance<JsonObject>()
    }

    private fun newestOf(names: List<String>): String? {
        val versioned = names.mapNotNull { name -> versionOf(name)?.let { version -> version to name } }
        val newest = versioned.maxByOrNull { (version, _) -> version }
        return newest?.second ?: names.firstOrNull()
    }

    private fun versionOf(name: String): SemanticVersion? {
        val withoutPrefix = name.removePrefix("v")
        return SemanticVersion.parseOrNull(withoutPrefix)
    }
}
