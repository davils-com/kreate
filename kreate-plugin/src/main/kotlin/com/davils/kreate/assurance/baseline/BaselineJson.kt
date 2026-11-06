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
package com.davils.kreate.assurance.baseline

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.objectOf
import com.davils.kreate.assurance.finding.optionalInt
import com.davils.kreate.assurance.finding.optionalText
import com.davils.kreate.assurance.finding.rejectUnknownFields
import com.davils.kreate.assurance.finding.requiredText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SCHEMA_VERSION: Int = 1

private val PRETTY_JSON: Json = Json { prettyPrint = true }

private const val FIELD_SOURCE: String = "source"

private const val FIELD_RULE: String = "rule"

private const val FIELD_MODULE: String = "module"

private const val FIELD_PATH: String = "path"

private const val FIELD_COUNT: String = "count"

private const val FIELD_SEVERITY: String = "severity"

private const val FIELD_TITLE: String = "title"

private val ENTRY_FIELDS: Set<String> = setOf(
    FIELD_SOURCE,
    FIELD_RULE,
    FIELD_MODULE,
    FIELD_PATH,
    FIELD_COUNT,
    FIELD_SEVERITY,
    FIELD_TITLE
)

internal object BaselineJson {
    fun render(entries: List<BaselineEntry>): String {
        val sorted = entries.sortedBy { entry -> entry.key }
        val json = buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("entries", entriesOf(sorted))
        }
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), json) + "\n"
    }

    fun parse(text: String, location: String): List<BaselineEntry> {
        val json = parseObject(text, location)
        val entries = json["entries"] as? JsonArray
            ?: throw AssuranceInputException("$location needs a list of entries.")
        return entries.mapIndexed { index, entry ->
            val entryLocation = "$location entry $index"
            readEntry(objectOf(entry, entryLocation), entryLocation)
        }
    }

    private fun entriesOf(entries: List<BaselineEntry>): JsonArray = buildJsonArray {
        entries.forEach { entry -> add(writeEntry(entry)) }
    }

    private fun writeEntry(entry: BaselineEntry): JsonObject = buildJsonObject {
        put(FIELD_SOURCE, entry.source)
        put(FIELD_RULE, entry.rule)
        entry.module?.let { module -> put(FIELD_MODULE, module) }
        entry.path?.let { path -> put(FIELD_PATH, path) }
        put(FIELD_COUNT, entry.count)
        put(FIELD_SEVERITY, entry.severity.name)
        put(FIELD_TITLE, entry.title)
    }

    private fun parseObject(text: String, location: String): JsonObject {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (cause: SerializationException) {
            throw AssuranceInputException("$location is not valid JSON.", cause)
        }
        return objectOf(element, location)
    }

    private fun readEntry(json: JsonObject, location: String): BaselineEntry {
        json.rejectUnknownFields(ENTRY_FIELDS, location)

        val count = json.optionalInt(FIELD_COUNT, location)
        if (count == null || count < 1) throw AssuranceInputException("$location needs a count of at least 1.")

        return BaselineEntry(
            source = json.requiredText(FIELD_SOURCE, location),
            rule = json.requiredText(FIELD_RULE, location),
            count = count,
            severity = severityOf(json.requiredText(FIELD_SEVERITY, location), location),
            title = json.requiredText(FIELD_TITLE, location),
            module = json.optionalText(FIELD_MODULE, location),
            path = json.optionalText(FIELD_PATH, location)
        )
    }

    private fun severityOf(name: String, location: String): Severity =
        Severity.entries.firstOrNull { severity -> severity.name == name }
            ?: throw AssuranceInputException("$location has the unknown severity \"$name\".")
}
