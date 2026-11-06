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
package com.davils.kreate.trivy.report

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.text(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (!primitive.isString) return null
    return primitive.content.ifEmpty { null }
}

internal fun JsonObject.number(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.intOrNull
}

internal fun JsonObject.objects(key: String): List<JsonObject> {
    val entries = this[key] as? JsonArray ?: return emptyList()
    return entries.filterIsInstance<JsonObject>()
}
