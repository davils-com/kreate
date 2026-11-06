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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.format.DateTimeParseException

private const val SCHEMA_VERSION: Int = 1

private val PRETTY_JSON: Json = Json { prettyPrint = true }

internal object FindingDocumentJson {
    fun render(document: FindingDocument): String {
        val json = buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("source", document.source.id)
            put("status", document.status.name)
            put("evaluatedAt", document.evaluatedAt.toString())
            document.reason?.let { reason -> put("reason", reason) }
            document.dataAsOf?.let { dataAsOf -> put("dataAsOf", dataAsOf.toString()) }
            put("findings", findingsOf(document))
        }
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), json) + "\n"
    }

    fun parse(text: String, location: String): FindingDocument {
        val json = parseObject(text, location)
        val sourceId = json.requiredText("source", location)
        val source = FindingSource.parse(sourceId)
            ?: throw AssuranceInputException("$location has the unknown source \"$sourceId\".")

        return FindingDocument(
            source = source,
            status = statusOf(json, location),
            evaluatedAt = instantOf(json.requiredText("evaluatedAt", location), location),
            findings = findingsOf(json, location),
            reason = json.optionalText("reason", location),
            dataAsOf = json.optionalText("dataAsOf", location)?.let { text -> instantOf(text, location) }
        )
    }

    private fun findingsOf(document: FindingDocument): JsonArray = buildJsonArray {
        document.findings.forEach { finding -> add(FindingJson.write(finding)) }
    }

    private fun parseObject(text: String, location: String): JsonObject {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (cause: SerializationException) {
            throw AssuranceInputException("$location is not valid JSON.", cause)
        }
        return objectOf(element, location)
    }

    private fun statusOf(json: JsonObject, location: String): DocumentStatus {
        val name = json.requiredText("status", location)
        return DocumentStatus.entries.firstOrNull { status -> status.name == name }
            ?: throw AssuranceInputException("$location has the unknown status \"$name\".")
    }

    private fun instantOf(text: String, location: String): Instant = try {
        Instant.parse(text)
    } catch (cause: DateTimeParseException) {
        throw AssuranceInputException("$location has the invalid time \"$text\".", cause)
    }

    private fun findingsOf(json: JsonObject, location: String): List<Finding> {
        val findings = json["findings"] ?: return emptyList()
        val entries = findings as? JsonArray
            ?: throw AssuranceInputException("$location has findings that are no list.")
        return entries.mapIndexed { index, entry ->
            val entryLocation = "$location finding $index"
            FindingJson.read(objectOf(entry, entryLocation), entryLocation)
        }
    }
}
