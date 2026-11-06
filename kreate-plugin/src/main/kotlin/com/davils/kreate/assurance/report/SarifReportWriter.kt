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
package com.davils.kreate.assurance.report

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SARIF_SCHEMA: String = "https://json.schemastore.org/sarif-2.1.0.json"

private const val SARIF_VERSION: String = "2.1.0"

private const val TOOL_NAME: String = "Kreate"

private const val TOOL_URI: String = "https://github.com/davils-com/kreate"

private const val FINGERPRINT_KEY: String = "kreate/v1"

private const val VERSION_KEY: String = "version"

private val PRETTY_JSON: Json = Json { prettyPrint = true }

internal object SarifReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val document = buildJsonObject {
            put("\$schema", SARIF_SCHEMA)
            put(VERSION_KEY, SARIF_VERSION)
            put("runs", buildJsonArray { add(runOf(evaluation)) })
        }
        return PRETTY_JSON.encodeToString(JsonObject.serializer(), document) + "\n"
    }

    private fun runOf(evaluation: AssuranceEvaluation): JsonObject = buildJsonObject {
        val findings = evaluation.outcome.kept
        put("tool", toolOf(evaluation, findings))
        put("automationDetails", buildJsonObject { put("id", "kreate/assurance/") })
        put("results", buildJsonArray { findings.forEach { finding -> add(resultOf(finding)) } })
    }

    private fun toolOf(evaluation: AssuranceEvaluation, findings: List<Finding>): JsonObject {
        val distinctRules = findings.distinctBy { finding -> finding.ruleId }
        val driver = buildJsonObject {
            put("name", TOOL_NAME)
            put(VERSION_KEY, evaluation.toolVersion)
            put("informationUri", TOOL_URI)
            put("rules", buildJsonArray { distinctRules.forEach { finding -> add(SarifRules.ruleOf(finding)) } })
        }
        return buildJsonObject { put("driver", driver) }
    }

    private fun levelOf(severity: Severity): String = when (severity) {
        Severity.CRITICAL, Severity.HIGH -> "error"
        Severity.MEDIUM -> "warning"
        Severity.LOW, Severity.UNKNOWN -> "note"
    }

    private fun resultOf(finding: Finding): JsonObject = buildJsonObject {
        put("ruleId", finding.ruleId)
        put("level", levelOf(finding.gatingSeverity))
        put("message", textOf(messageOf(finding)))
        putLocation(finding)
        put("partialFingerprints", buildJsonObject { put(FINGERPRINT_KEY, finding.fingerprint) })
        put("properties", resultPropertiesOf(finding))
    }

    private fun messageOf(finding: Finding): String {
        if (finding.description.isEmpty()) return finding.title
        return "${finding.title}\n\n${finding.description}"
    }

    private fun JsonObjectBuilder.putLocation(finding: Finding) {
        val location = finding.location ?: return
        val physical = buildJsonObject {
            put("artifactLocation", buildJsonObject { put("uri", location.path) })
            location.line?.let { line -> put("region", buildJsonObject { put("startLine", line) }) }
        }
        val entry = buildJsonObject { put("physicalLocation", physical) }
        put("locations", buildJsonArray { add(entry) })
    }

    private fun resultPropertiesOf(finding: Finding): JsonObject = buildJsonObject {
        put("severity", finding.severity.name)
        put("source", finding.source.id)
        finding.module?.let { module -> put("module", module) }
        finding.version?.let { version -> put(VERSION_KEY, version) }
        finding.fixedVersion?.let { fixed -> put("fixedVersion", fixed) }
    }

    private fun textOf(text: String): JsonObject = SarifRules.textOf(text)
}
