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
package com.davils.kreate.standard.lock

import com.davils.kore.core.text.SafeText
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.objectOf
import com.davils.kreate.assurance.finding.optionalInt
import com.davils.kreate.assurance.finding.optionalText
import com.davils.kreate.assurance.finding.rejectUnknownFields
import com.davils.kreate.assurance.finding.requiredText
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIdGrammar
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val SCHEMA_VERSION: Int = 1

private val PRETTY_JSON: Json = Json { prettyPrint = true }

private const val FIELD_SCHEMA_VERSION: String = "schemaVersion"

private const val FIELD_STANDARDS: String = "standards"

private const val FIELD_ID: String = "id"

private const val FIELD_REVIEWED_ON: String = "reviewedOn"

private const val FIELD_NOTE: String = "note"

private const val FIELD_SNAPSHOT: String = "snapshot"

private val DOCUMENT_FIELDS: Set<String> = setOf(FIELD_SCHEMA_VERSION, FIELD_STANDARDS)

private val ENTRY_FIELDS: Set<String> = setOf(FIELD_ID, FIELD_REVIEWED_ON, FIELD_NOTE, FIELD_SNAPSHOT)

internal object StandardsLockJson {
    fun render(lock: StandardsLock): String {
        val sorted = lock.entries.sortedBy { entry -> entry.id }
        val json = buildJsonObject {
            put(FIELD_SCHEMA_VERSION, SCHEMA_VERSION)
            put(FIELD_STANDARDS, entriesOf(sorted))
        }
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), json) + "\n"
    }

    fun parse(text: String, location: String): StandardsLock {
        val json = parseObject(text, location)
        json.rejectUnknownFields(DOCUMENT_FIELDS, location)
        val version = json.optionalInt(FIELD_SCHEMA_VERSION, location)
        if (version != SCHEMA_VERSION) throw AssuranceInputException("$location needs schemaVersion $SCHEMA_VERSION.")
        val standards = json[FIELD_STANDARDS] as? JsonArray
            ?: throw AssuranceInputException("$location needs a list of standards.")
        val entries = standards.mapIndexed { index, entry ->
            val entryLocation = "$location standard $index"
            readEntry(objectOf(entry, entryLocation), entryLocation)
        }
        return StandardsLock(entries)
    }

    private fun entriesOf(entries: List<LockEntry>): JsonArray = buildJsonArray {
        entries.forEach { entry -> add(writeEntry(entry)) }
    }

    private fun writeEntry(entry: LockEntry): JsonObject = buildJsonObject {
        put(FIELD_ID, entry.id.toString())
        put(FIELD_REVIEWED_ON, entry.reviewedOn.toString())
        entry.note?.let { note -> put(FIELD_NOTE, note) }
        put(FIELD_SNAPSHOT, SnapshotJson.write(entry.snapshot))
    }

    private fun parseObject(text: String, location: String): JsonObject {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (cause: SerializationException) {
            throw AssuranceInputException("$location is not valid JSON.", cause)
        }
        return objectOf(element, location)
    }

    private fun readEntry(json: JsonObject, location: String): LockEntry {
        json.rejectUnknownFields(ENTRY_FIELDS, location)
        val snapshot = json[FIELD_SNAPSHOT] ?: throw AssuranceInputException("$location needs a snapshot.")
        return LockEntry(
            id = idOf(json.requiredText(FIELD_ID, location), location),
            reviewedOn = dateOf(json.requiredText(FIELD_REVIEWED_ON, location), location),
            snapshot = SnapshotJson.read(snapshot, "$location snapshot"),
            note = json.optionalText(FIELD_NOTE, location)
        )
    }

    private fun idOf(text: String, location: String): StandardId =
        StandardIdGrammar.parse(text)
            ?: throw AssuranceInputException("$location has the unknown id ${SafeText.quoted(text)}.")

    private fun dateOf(text: String, location: String): LocalDate = try {
        LocalDate.parse(text)
    } catch (cause: DateTimeParseException) {
        throw AssuranceInputException("$location needs a reviewedOn date of the form YYYY-MM-DD.", cause)
    }
}
