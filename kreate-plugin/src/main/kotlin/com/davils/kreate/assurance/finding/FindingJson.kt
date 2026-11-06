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
package com.davils.kreate.assurance.finding

import com.davils.kreate.assurance.report.SnippetSanitizer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object FindingJson {
    fun write(finding: Finding): JsonObject = buildJsonObject {
        put("ruleId", finding.ruleId)
        put("source", finding.source.id)
        put("severity", finding.severity.name)
        put("title", finding.title)
        putText("description", finding.description.ifEmpty { null })
        putText("file", finding.location?.path)
        finding.location?.line?.let { line -> put("line", line) }
        putText("module", finding.module)
        putText("version", finding.version)
        putText("fixedVersion", finding.fixedVersion)
        putText("url", finding.url)
        putText("snippet", finding.snippet)
        if (finding.aliases.isNotEmpty()) put("aliases", textArray(finding.aliases))
        if (finding.attributes.isNotEmpty()) put("attributes", textObject(finding.attributes))
        put("fingerprint", finding.fingerprint)
    }

    fun read(json: JsonObject, location: String): Finding {
        val sourceId = json.requiredText("source", location)
        val source = FindingSource.parse(sourceId)
            ?: throw AssuranceInputException("$location has the unknown source \"$sourceId\".")

        return Finding(
            ruleId = json.requiredText("ruleId", location),
            source = source,
            severity = parseSeverity(json.requiredText("severity", location)),
            title = SnippetSanitizer.escape(json.requiredText("title", location)),
            description = SnippetSanitizer.escape(json.optionalText("description", location).orEmpty()),
            location = locationOf(json, location),
            module = json.optionalText("module", location),
            version = json.optionalText("version", location),
            fixedVersion = json.optionalText("fixedVersion", location),
            aliases = textsOf(json, location),
            url = json.optionalText("url", location),
            snippet = json.optionalText("snippet", location)?.let(SnippetSanitizer::sanitize),
            attributes = attributesOf(json, location)
        )
    }

    private fun JsonObjectBuilder.putText(key: String, value: String?) {
        if (value != null) put(key, value)
    }

    private fun textArray(values: List<String>): JsonArray = buildJsonArray {
        values.forEach { value -> add(JsonPrimitive(value)) }
    }

    private fun textObject(values: Map<String, String>): JsonObject = buildJsonObject {
        values.forEach { (key, value) -> put(key, value) }
    }

    private fun locationOf(json: JsonObject, location: String): SourceLocation? {
        val path = json.optionalText("file", location) ?: return null
        return SourceLocation(path, json.optionalInt("line", location))
    }

    private fun textsOf(json: JsonObject, location: String): List<String> {
        val aliases = json["aliases"] ?: return emptyList()
        val entries = aliases as? JsonArray ?: throw AssuranceInputException("$location has aliases that are no list.")
        return entries.map { entry -> (entry as? JsonPrimitive)?.content.orEmpty() }
    }

    private fun attributesOf(json: JsonObject, location: String): Map<String, String> {
        val attributes = json["attributes"] ?: return emptyMap()
        val entries = objectOf(attributes, "$location attributes")
        return entries.mapValues { (_, value) -> (value as? JsonPrimitive)?.content.orEmpty() }
    }
}
