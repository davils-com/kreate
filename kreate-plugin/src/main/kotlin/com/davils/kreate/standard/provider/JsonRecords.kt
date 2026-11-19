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
package com.davils.kreate.standard.provider

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object JsonRecords {
    fun elementOf(text: String): JsonElement? = try {
        Json.parseToJsonElement(text)
    } catch (_: SerializationException) {
        null
    }

    fun objectOf(text: String): JsonObject? = elementOf(text) as? JsonObject

    fun JsonObject.text(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (!primitive.isString) return null
        return primitive.content
    }

    fun JsonObject.number(key: String): Long? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        return primitive.content.toLongOrNull()
    }

    fun JsonObject.texts(key: String): List<String> {
        val array = this[key] as? JsonArray ?: return emptyList()
        val primitives = array.filterIsInstance<JsonPrimitive>()
        val texts = primitives.filter { primitive -> primitive.isString }
        return texts.map { primitive -> primitive.content }
    }

    fun JsonObject.numbers(key: String): List<Long> {
        val array = this[key] as? JsonArray ?: return emptyList()
        val primitives = array.filterIsInstance<JsonPrimitive>()
        return primitives.mapNotNull { primitive -> primitive.content.toLongOrNull() }
    }

    fun JsonObject.child(key: String): JsonObject? = this[key] as? JsonObject
}
