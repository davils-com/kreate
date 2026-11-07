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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object AssurancePolicyWriter {
    fun render(policy: AssurancePolicy): String {
        val severities = policy.forbidIgnoreOf.map { severity -> JsonPrimitive(severity.name) }
        val entries = mutableMapOf<String, JsonElement>(
            SCHEMA_KEY to JsonPrimitive(SCHEMA_VERSION),
            NAME_KEY to JsonPrimitive(policy.name),
            REQUIRE_KEY to objectOf(policy.require),
            MINIMUM_KEY to objectOf(policy.minimum),
            FORBID_KEY to JsonArray(severities),
            EXPIRY_KEY to JsonPrimitive(policy.ignoreRulesNeedExpiry),
            DENIED_KEY to objectOf(policy.denied)
        )
        policy.maxIgnoreDays?.let { days -> entries[DAYS_KEY] = JsonPrimitive(days) }
        return JsonObject(entries).toString()
    }

    private fun objectOf(values: Map<String, String>): JsonObject =
        JsonObject(values.toSortedMap().mapValues { (_, value) -> JsonPrimitive(value) })
}
