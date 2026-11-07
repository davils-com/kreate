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
package com.davils.kreate.provenance.depsdev

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.format.DateTimeParseException

private const val SOURCE_REPOSITORY: String = "SOURCE_REPO"

internal object DepsDevParser {
    fun historyOf(text: String): PackageHistory {
        val versions = objectOf(text)["versions"] as? JsonArray ?: JsonArray(emptyList())
        val objects = versions.filterIsInstance<JsonObject>()
        val entries = objects.mapNotNull(::publicationOf)
        val latest = objects.firstOrNull { entry -> isDefault(entry) }
        return PackageHistory(entries.toMap(), latest?.let(::versionOf))
    }

    fun sourceProjectsOf(text: String): List<String> {
        val related = objectOf(text)["relatedProjects"] as? JsonArray ?: JsonArray(emptyList())
        val sources = related.filterIsInstance<JsonObject>().filter { project -> isSource(project) }
        return sources.mapNotNull { project -> projectIdOf(project) }
    }

    fun scorecardOf(text: String): Double? {
        val scorecard = objectOf(text)["scorecard"] as? JsonObject ?: return null
        val score = scorecard["overallScore"] as? JsonPrimitive ?: return null
        return score.doubleOrNull
    }

    private fun objectOf(text: String): JsonObject {
        val element = parse(text)
        return element as? JsonObject ?: throw DepsDevUnavailableException("deps.dev answered with no object.")
    }

    private fun parse(text: String): JsonElement = try {
        Json.parseToJsonElement(text)
    } catch (cause: SerializationException) {
        throw DepsDevUnavailableException("deps.dev answered with invalid JSON.", cause)
    }

    private fun isDefault(entry: JsonObject): Boolean {
        val flag = entry["isDefault"] as? JsonPrimitive
        return flag?.content == "true"
    }

    private fun versionOf(entry: JsonObject): String? {
        val key = entry["versionKey"] as? JsonObject ?: return null
        return (key["version"] as? JsonPrimitive)?.content
    }

    private fun publicationOf(entry: JsonObject): Pair<String, Instant>? {
        val version = versionOf(entry) ?: return null
        val published = (entry["publishedAt"] as? JsonPrimitive)?.content ?: return null
        val instant = instantOf(published) ?: return null
        return version to instant
    }

    private fun instantOf(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun isSource(project: JsonObject): Boolean {
        val relation = project["relationType"] as? JsonPrimitive
        return relation?.content == SOURCE_REPOSITORY
    }

    private fun projectIdOf(project: JsonObject): String? {
        val key = project["projectKey"] as? JsonObject ?: return null
        return (key["id"] as? JsonPrimitive)?.content
    }
}
