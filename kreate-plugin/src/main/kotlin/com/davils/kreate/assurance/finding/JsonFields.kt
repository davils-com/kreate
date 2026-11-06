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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.requiredText(key: String, location: String): String =
    optionalText(key, location) ?: throw AssuranceInputException("$location needs a \"$key\" text.")

internal fun JsonObject.optionalText(key: String, location: String): String? {
    val value = this[key]
    if (value == null || value is JsonNull) return null

    val primitive = value as? JsonPrimitive
    if (primitive == null || !primitive.isString) {
        throw AssuranceInputException("$location has a \"$key\" that is not a text.")
    }
    return primitive.content
}

internal fun JsonObject.optionalInt(key: String, location: String): Int? {
    val value = this[key]
    if (value == null || value is JsonNull) return null

    val primitive = value as? JsonPrimitive
    return primitive?.intOrNull ?: throw AssuranceInputException("$location has a \"$key\" that is not a number.")
}

internal fun JsonObject.rejectUnknownFields(known: Set<String>, location: String) {
    val unknown = keys.firstOrNull { key -> key !in known } ?: return
    throw AssuranceInputException("$location has the unknown field \"$unknown\".")
}

internal fun objectOf(element: JsonElement, location: String): JsonObject =
    element as? JsonObject ?: throw AssuranceInputException("$location must be an object.")
