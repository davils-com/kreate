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
package com.davils.kreate.inspection.scan

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val RULE_KEY: String = "rule"

private const val PATH_KEY: String = "path"

private const val DETAIL_KEY: String = "detail"

internal object HitsJson {
    fun render(hits: Collection<Hit>): String {
        val entries = buildJsonArray { hits.forEach { hit -> add(entryOf(hit)) } }
        return entries.toString()
    }

    fun parse(text: String): List<Hit> {
        val entries = Json.parseToJsonElement(text) as JsonArray
        return entries.filterIsInstance<JsonObject>().map(::hitOf)
    }

    private fun entryOf(hit: Hit): JsonObject = buildJsonObject {
        put(RULE_KEY, hit.rule)
        put(PATH_KEY, hit.path)
        hit.detail?.let { detail -> put(DETAIL_KEY, detail) }
    }

    private fun hitOf(entry: JsonObject): Hit {
        val rule = (entry[RULE_KEY] as JsonPrimitive).content
        val path = (entry[PATH_KEY] as JsonPrimitive).content
        return Hit(rule, path, (entry[DETAIL_KEY] as? JsonPrimitive)?.content)
    }
}
