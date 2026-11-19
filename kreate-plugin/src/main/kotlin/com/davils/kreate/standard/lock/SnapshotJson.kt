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
import com.davils.kreate.assurance.finding.optionalText
import com.davils.kreate.assurance.finding.rejectUnknownFields
import com.davils.kreate.assurance.finding.requiredText
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.StandardSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val FIELD_TITLE: String = "title"

private const val FIELD_STATUS: String = "status"

private const val FIELD_REVISION: String = "revision"

private const val FIELD_PUBLISHED: String = "published"

private const val FIELD_WITHDRAWN: String = "withdrawn"

private const val FIELD_REPLACED_BY: String = "replacedBy"

private const val FIELD_UPDATED_BY: String = "updatedBy"

private const val FIELD_ERRATA: String = "errata"

private const val FIELD_FINGERPRINT: String = "fingerprint"

private val SNAPSHOT_FIELDS: Set<String> = setOf(
    FIELD_TITLE,
    FIELD_STATUS,
    FIELD_REVISION,
    FIELD_PUBLISHED,
    FIELD_WITHDRAWN,
    FIELD_REPLACED_BY,
    FIELD_UPDATED_BY,
    FIELD_ERRATA,
    FIELD_FINGERPRINT
)

internal object SnapshotJson {
    fun write(snapshot: StandardSnapshot): JsonObject = buildJsonObject {
        put(FIELD_TITLE, snapshot.title)
        snapshot.status?.let { status -> put(FIELD_STATUS, status) }
        snapshot.revision?.let { revision -> put(FIELD_REVISION, revision) }
        snapshot.published?.let { published -> put(FIELD_PUBLISHED, published) }
        put(FIELD_WITHDRAWN, snapshot.withdrawn)
        put(FIELD_REPLACED_BY, textsOf(snapshot.replacedBy))
        put(FIELD_UPDATED_BY, textsOf(snapshot.updatedBy))
        put(FIELD_ERRATA, errataOf(snapshot.errata))
        snapshot.fingerprint?.let { fingerprint -> put(FIELD_FINGERPRINT, fingerprint) }
    }

    fun read(element: JsonElement, location: String): StandardSnapshot {
        val json = objectOf(element, location)
        json.rejectUnknownFields(SNAPSHOT_FIELDS, location)
        return StandardSnapshot(
            title = json.requiredText(FIELD_TITLE, location),
            status = json.optionalText(FIELD_STATUS, location),
            revision = json.optionalText(FIELD_REVISION, location),
            published = json.optionalText(FIELD_PUBLISHED, location),
            withdrawn = withdrawnOf(json, location),
            replacedBy = textsIn(json, FIELD_REPLACED_BY, location),
            updatedBy = textsIn(json, FIELD_UPDATED_BY, location),
            errata = errataIn(json, location),
            fingerprint = json.optionalText(FIELD_FINGERPRINT, location)
        )
    }

    private fun textsOf(texts: List<String>): JsonArray = buildJsonArray {
        texts.sorted().forEach { text -> add(JsonPrimitive(text)) }
    }

    private fun errataOf(errata: Map<String, ErratumState>): JsonObject = buildJsonObject {
        errata.keys.sorted().forEach { id -> put(id, errata.getValue(id).name) }
    }

    private fun withdrawnOf(json: JsonObject, location: String): Boolean {
        val value = json[FIELD_WITHDRAWN]
        if (value == null || value is JsonNull) return false
        val primitive = value as? JsonPrimitive
        return primitive?.booleanOrNull
            ?: throw AssuranceInputException("$location has a \"$FIELD_WITHDRAWN\" that is not true or false.")
    }

    private fun textsIn(json: JsonObject, key: String, location: String): List<String> {
        val value = json[key]
        if (value == null || value is JsonNull) return emptyList()
        val array = value as? JsonArray ?: throw AssuranceInputException("$location has a \"$key\" that is no list.")
        return array.map { element -> textOf(element, "$location $key") }
    }

    private fun errataIn(json: JsonObject, location: String): Map<String, ErratumState> {
        val value = json[FIELD_ERRATA]
        if (value == null || value is JsonNull) return emptyMap()
        val errata = objectOf(value, "$location $FIELD_ERRATA")
        return errata.mapValues { (id, state) -> stateOf(textOf(state, "$location erratum $id"), location) }
    }

    private fun textOf(element: JsonElement, location: String): String {
        val primitive = element as? JsonPrimitive
        if (primitive == null || !primitive.isString) throw AssuranceInputException("$location holds no text.")
        return primitive.content
    }

    private fun stateOf(name: String, location: String): ErratumState =
        ErratumState.parse(name)
            ?: throw AssuranceInputException("$location has the unknown erratum state ${SafeText.quoted(name)}.")
}
