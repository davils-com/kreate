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
package com.davils.kreate.policy.model

import com.davils.kreate.assurance.Severity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

internal const val SCHEMA_VERSION: Int = 1

internal const val SCHEMA_KEY: String = "schemaVersion"

internal const val NAME_KEY: String = "name"

internal const val REQUIRE_KEY: String = "require"

internal const val MINIMUM_KEY: String = "minimum"

internal const val FORBID_KEY: String = "forbidIgnoreOf"

internal const val EXPIRY_KEY: String = "ignoreRulesNeedExpiry"

internal const val DAYS_KEY: String = "maxIgnoreDays"

internal const val DENIED_KEY: String = "denied"

private val KNOWN_KEYS: Set<String> =
    setOf(SCHEMA_KEY, NAME_KEY, REQUIRE_KEY, MINIMUM_KEY, FORBID_KEY, EXPIRY_KEY, DAYS_KEY, DENIED_KEY)

internal object AssurancePolicyJson {
    fun parse(text: String): AssurancePolicy {
        val root = rootOf(text)
        return AssurancePolicy(
            name = textOf(root[NAME_KEY], NAME_KEY) ?: throw PolicyFormatException("The policy needs a name."),
            require = mapOf(root[REQUIRE_KEY], REQUIRE_KEY),
            minimum = mapOf(root[MINIMUM_KEY], MINIMUM_KEY),
            forbidIgnoreOf = severitiesOf(root[FORBID_KEY]),
            ignoreRulesNeedExpiry = booleanOf(root[EXPIRY_KEY]),
            maxIgnoreDays = daysOf(root[DAYS_KEY]),
            denied = mapOf(root[DENIED_KEY], DENIED_KEY)
        )
    }

    private fun rootOf(text: String): JsonObject {
        val root = parsed(text) as? JsonObject ?: throw PolicyFormatException("The policy is no JSON object.")
        val unknown = root.keys - KNOWN_KEYS
        val version = (root[SCHEMA_KEY] as? JsonPrimitive)?.intOrNull
        val problem = problemOf(unknown, version) ?: return root
        throw PolicyFormatException(problem)
    }

    private fun problemOf(unknown: Set<String>, version: Int?): String? {
        if (unknown.isNotEmpty()) return "The policy has unknown keys: ${unknown.sorted()}."
        if (version != SCHEMA_VERSION) return "The policy needs schemaVersion $SCHEMA_VERSION."
        return null
    }

    private fun parsed(text: String): JsonElement? = try {
        Json.parseToJsonElement(text)
    } catch (_: SerializationException) {
        null
    }

    private fun textOf(element: JsonElement?, key: String): String? {
        if (element == null) return null
        val primitive = element as? JsonPrimitive ?: throw PolicyFormatException("$key must be a value.")
        return primitive.content
    }

    private fun mapOf(element: JsonElement?, key: String): Map<String, String> {
        if (element == null) return emptyMap()
        val values = element as? JsonObject ?: throw PolicyFormatException("$key must be an object.")
        return values.mapValues { (name, value) -> textOf(value, "$key.$name").orEmpty() }
    }

    private fun severitiesOf(element: JsonElement?): Set<Severity> {
        if (element == null) return emptySet()
        val values = element as? JsonArray ?: throw PolicyFormatException("$FORBID_KEY must be a list.")
        return values.map { value -> severityOf(textOf(value, FORBID_KEY).orEmpty()) }.toSet()
    }

    private fun severityOf(name: String): Severity {
        val severity = Severity.entries.firstOrNull { candidate -> candidate.name == name.uppercase() }
        return severity ?: throw PolicyFormatException("'$name' is no severity.")
    }

    private fun booleanOf(element: JsonElement?): Boolean {
        if (element == null) return false
        val flag = (element as? JsonPrimitive)?.booleanOrNull
        return flag ?: throw PolicyFormatException("$EXPIRY_KEY must be true or false.")
    }

    private fun daysOf(element: JsonElement?): Int? {
        if (element == null) return null
        val days = (element as? JsonPrimitive)?.intOrNull
            ?: throw PolicyFormatException("$DAYS_KEY must be a number.")
        if (days < 0) throw PolicyFormatException("$DAYS_KEY must not be negative.")
        return days
    }
}
