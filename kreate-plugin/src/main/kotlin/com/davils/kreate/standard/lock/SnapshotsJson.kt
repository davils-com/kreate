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
import com.davils.kreate.standard.model.StandardId
import com.davils.kreate.standard.model.StandardIdGrammar
import com.davils.kreate.standard.model.StandardSnapshot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val PRETTY_JSON: Json = Json { prettyPrint = true }

private const val FIELD_SNAPSHOTS: String = "snapshots"

internal object SnapshotsJson {
    fun render(snapshots: Map<StandardId, StandardSnapshot>): String {
        val json = buildJsonObject {
            put(FIELD_SNAPSHOTS, snapshotsOf(snapshots))
        }
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), json) + "\n"
    }

    fun parse(text: String, location: String): Map<StandardId, StandardSnapshot> {
        val element = try {
            Json.parseToJsonElement(text)
        } catch (cause: SerializationException) {
            throw AssuranceInputException("$location is not valid JSON.", cause)
        }
        val json = objectOf(element, location)
        val snapshots = json[FIELD_SNAPSHOTS] ?: throw AssuranceInputException("$location holds no snapshots.")
        val byId = objectOf(snapshots, "$location $FIELD_SNAPSHOTS")
        val entries = byId.entries.map { (id, snapshot) ->
            idOf(id, location) to SnapshotJson.read(snapshot, "$location $id")
        }
        return entries.toMap()
    }

    private fun snapshotsOf(snapshots: Map<StandardId, StandardSnapshot>): JsonObject = buildJsonObject {
        snapshots.keys.sorted().forEach { id -> put(id.toString(), SnapshotJson.write(snapshots.getValue(id))) }
    }

    private fun idOf(text: String, location: String): StandardId =
        StandardIdGrammar.parse(text)
            ?: throw AssuranceInputException("$location has the unknown id ${SafeText.quoted(text)}.")
}
