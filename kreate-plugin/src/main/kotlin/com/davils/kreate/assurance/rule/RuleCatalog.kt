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
package com.davils.kreate.assurance.rule

import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.parseSeverity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val CATALOG_RESOURCES: Map<FindingSource, String> = mapOf(
    FindingSource.INSPECTION to "/assurance/rules/inspection.json",
    FindingSource.TRUST to "/assurance/rules/trust.json",
    FindingSource.CONFUSION to "/assurance/rules/confusion.json",
    FindingSource.DEPENDENCIES to "/assurance/rules/dependencies.json",
    FindingSource.CONFIG to "/assurance/rules/config.json",
    FindingSource.WORKSPACE to "/assurance/rules/workspace.json",
    FindingSource.POLICY to "/assurance/rules/policy.json",
    FindingSource.QUALITY to "/assurance/rules/quality.json"
)

internal object RuleCatalog {
    val rules: List<RuleInfo> by lazy {
        CATALOG_RESOURCES.flatMap { (source, resource) -> load(source, resource) }
    }

    private val byId: Map<String, RuleInfo> by lazy { rules.associateBy { rule -> rule.id } }

    fun find(id: String): RuleInfo? = byId[id]

    fun require(id: String): RuleInfo = find(id) ?: error("The rule $id is missing from the catalog.")

    private fun load(source: FindingSource, resource: String): List<RuleInfo> {
        val stream = RuleCatalog::class.java.getResourceAsStream(resource)
            ?: error("The rule catalog $resource is missing.")
        val text = stream.use { input -> input.readBytes().decodeToString() }
        val document = Json.parseToJsonElement(text) as JsonObject
        val entries = document.getValue("rules") as JsonArray
        return entries.map { entry -> ruleOf(entry as JsonObject, source) }
    }

    private fun ruleOf(entry: JsonObject, source: FindingSource): RuleInfo = RuleInfo(
        id = entry.text("id"),
        source = source,
        severity = parseSeverity(entry.text("severity")),
        category = entry.text("category"),
        summary = entry.text("summary"),
        explanation = entry.text("explanation")
    )

    private fun JsonObject.text(key: String): String {
        val value = getValue(key) as JsonPrimitive
        return value.content
    }
}
