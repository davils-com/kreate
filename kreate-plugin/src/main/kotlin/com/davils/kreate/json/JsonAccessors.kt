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

package com.davils.kreate.json

internal val JsonValue.typeName: String
    get() = when (this) {
        JsonNull -> "null"
        is JsonBoolean -> "boolean"
        is JsonNumber -> "number"
        is JsonString -> "string"
        is JsonArray -> "array"
        is JsonObject -> "object"
    }

internal fun JsonValue?.asObject(): JsonObject? = this as? JsonObject

internal fun JsonValue?.asArray(): List<JsonValue>? = (this as? JsonArray)?.elements

internal fun JsonValue?.asString(): String? = (this as? JsonString)?.value

internal fun JsonValue?.asBoolean(): Boolean? = (this as? JsonBoolean)?.value

internal fun JsonValue?.asLong(): Long? = (this as? JsonNumber)?.toLongOrNull()

internal fun JsonValue?.asDouble(): Double? = (this as? JsonNumber)?.toDouble()

internal fun JsonObject.obj(key: String): JsonObject? = members[key].asObject()

internal fun JsonObject.array(key: String): List<JsonValue>? = members[key].asArray()

internal fun JsonObject.string(key: String): String? = members[key].asString()

internal fun JsonObject.boolean(key: String): Boolean? = members[key].asBoolean()

internal fun JsonObject.long(key: String): Long? = members[key].asLong()

internal fun JsonObject.double(key: String): Double? = members[key].asDouble()
