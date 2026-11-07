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
package com.davils.kreate.assurance.ingest

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.report.SnippetSanitizer
import com.davils.kreate.assurance.rule.RuleCatalog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private const val RULE: String = "DETEKT_ISSUE"

internal object SarifIngest {
    fun findingsOf(text: String, projectUri: String): List<Finding> {
        val root = parsed(text) as? JsonObject ?: return emptyList()
        val runs = root["runs"] as? JsonArray ?: return emptyList()
        val results = runs.filterIsInstance<JsonObject>().flatMap { run -> resultsOf(run) }
        return results.map { result -> findingOf(result, projectUri) }
    }

    private fun parsed(text: String): JsonElement? = try {
        Json.parseToJsonElement(text)
    } catch (_: SerializationException) {
        null
    }

    private fun resultsOf(run: JsonObject): List<JsonObject> {
        val results = run["results"] as? JsonArray ?: return emptyList()
        return results.filterIsInstance<JsonObject>()
    }

    private fun findingOf(result: JsonObject, projectUri: String): Finding {
        val ruleId = textOf(result["ruleId"]) ?: RULE
        val message = (result["message"] as? JsonObject)?.let { textOf(it["text"]) }.orEmpty()
        val explanation = RuleCatalog.require(RULE).explanation
        return Finding(
            ruleId = ruleId.substringAfterLast('/'),
            source = FindingSource.QUALITY,
            severity = severityOf(textOf(result["level"])),
            title = SnippetSanitizer.escape(message.ifEmpty { ruleId }),
            description = explanation,
            location = locationOf(result, projectUri),
            attributes = mapOf(REPORT_ONLY_ATTRIBUTE to "true", "tool" to "detekt", "rule" to ruleId)
        )
    }

    private fun locationOf(result: JsonObject, projectUri: String): SourceLocation? {
        val locations = result["locations"] as? JsonArray ?: return null
        val physical = (locations.firstOrNull() as? JsonObject)?.get("physicalLocation") as? JsonObject ?: return null
        val uri = (physical["artifactLocation"] as? JsonObject)?.let { textOf(it["uri"]) } ?: return null
        val line = ((physical["region"] as? JsonObject)?.get("startLine") as? JsonPrimitive)?.intOrNull
        return SourceLocation(uri.removePrefix(projectUri).removePrefix("/"), line)
    }

    private fun severityOf(level: String?): Severity {
        if (level == "error") return Severity.MEDIUM
        return Severity.LOW
    }

    private fun textOf(element: JsonElement?): String? = (element as? JsonPrimitive)?.content
}
