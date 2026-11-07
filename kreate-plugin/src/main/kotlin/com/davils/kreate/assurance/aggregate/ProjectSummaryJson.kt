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
package com.davils.kreate.assurance.aggregate

import com.davils.kreate.assurance.finding.FindingJson
import com.davils.kreate.assurance.policy.GateStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal object ProjectSummaryJson {
    fun parse(text: String, location: String): ProjectSummary? {
        val root = parsed(text) as? JsonObject ?: return null
        val project = textOf(root["project"]) ?: return null
        val result = root["result"] as? JsonObject ?: return null
        val status = GateStatus.entries.firstOrNull { entry -> entry.name == textOf(result["status"]) } ?: return null
        val blocking = (result["blockingFindings"] as? JsonPrimitive)?.intOrNull ?: 0
        val incomplete = (result["incompleteSources"] as? JsonArray).orEmpty().mapNotNull(::textOf)
        val entries = (root["findings"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
        val findings = entries.mapIndexed { index, entry -> FindingJson.read(entry, "$location finding $index") }
        return ProjectSummary(project, status, blocking, incomplete, findings)
    }

    private fun parsed(text: String): JsonElement? = try {
        Json.parseToJsonElement(text)
    } catch (_: SerializationException) {
        null
    }

    private fun textOf(element: JsonElement?): String? = (element as? JsonPrimitive)?.content
}
