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
package com.davils.kreate.advisory.osv

import com.davils.kreate.advisory.severity.Cvss3
import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.parseSeverity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val CVSS_V3_TYPE: String = "CVSS_V3"

private const val ECOSYSTEM_RANGE: String = "ECOSYSTEM"

private const val INTRODUCED_EVENT: String = "introduced"

private const val FIXED_EVENT: String = "fixed"

internal object OsvRecordParser {
    fun parse(text: String, id: String): OsvRecord {
        val record = parseObject(text, id)
        return OsvRecord(
            id = record.text("id") ?: id,
            aliases = record.array("aliases").mapNotNull { alias -> (alias as? JsonPrimitive)?.content },
            summary = record.text("summary"),
            details = record.text("details"),
            modified = record.text("modified"),
            isWithdrawn = record.text("withdrawn") != null,
            severity = severityOf(record),
            ranges = rangesOf(record)
        )
    }

    private fun parseObject(text: String, id: String): JsonObject {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (cause: SerializationException) {
            throw OsvUnavailableException("The record of $id is not valid JSON.", cause)
        }
        return element as? JsonObject ?: throw OsvUnavailableException("The record of $id is not an object.")
    }

    private fun severityOf(record: JsonObject): Severity {
        val scored = record.objects("severity").filter { entry -> entry.text("type") == CVSS_V3_TYPE }
        val computed = scored.mapNotNull { entry -> entry.text("score")?.let(Cvss3::severityOf) }
        val mostSevere = computed.minOrNull()
        if (mostSevere != null) return mostSevere

        val specific = record["database_specific"] as? JsonObject
        return parseSeverity(specific?.text("severity").orEmpty())
    }

    private fun rangesOf(record: JsonObject): Map<String, List<OsvRange>> {
        val affected = record.objects("affected")
        val byPackage = affected.groupBy { entry -> packageNameOf(entry) }
        return byPackage.mapValues { (_, entries) -> entries.flatMap { entry -> rangesOfEntry(entry) } }
    }

    private fun packageNameOf(entry: JsonObject): String {
        val affectedPackage = entry["package"] as? JsonObject
        return affectedPackage?.text("name").orEmpty()
    }

    private fun rangesOfEntry(entry: JsonObject): List<OsvRange> {
        val ecosystemRanges = entry.objects("ranges").filter { range -> range.text("type") == ECOSYSTEM_RANGE }
        return ecosystemRanges.flatMap { range -> windowsOf(range.objects("events")) }
    }

    private fun windowsOf(events: List<JsonObject>): List<OsvRange> {
        val windows = mutableListOf<OsvRange>()
        var introduced: String? = null
        events.forEach { event ->
            event.text(INTRODUCED_EVENT)?.let { version -> introduced = version }
            event.text(FIXED_EVENT)?.let { version -> windows += OsvRange(introduced, version) }
        }
        return windows
    }

    private fun JsonObject.text(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (!primitive.isString) return null
        return primitive.content
    }

    private fun JsonObject.array(key: String): List<JsonElement> = this[key] as? JsonArray ?: emptyList()

    private fun JsonObject.objects(key: String): List<JsonObject> = array(key).filterIsInstance<JsonObject>()
}
