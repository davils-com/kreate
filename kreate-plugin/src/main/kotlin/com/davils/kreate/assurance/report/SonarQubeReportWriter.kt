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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val PRETTY_JSON: Json = Json { prettyPrint = true }

private const val ENGINE: String = "kreate"

internal object SonarQubeReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val kept = evaluation.outcome.kept
        val rules = kept.distinctBy { finding -> idOf(finding) }.map(::ruleOf)
        val report = buildJsonObject {
            put("rules", JsonArray(rules))
            put("issues", JsonArray(kept.map(::issueOf)))
        }
        return PRETTY_JSON.encodeToString(JsonElement.serializer(), report) + "\n"
    }

    private fun idOf(finding: Finding): String = "${finding.source.id}.${finding.ruleId}"

    private fun ruleOf(finding: Finding): JsonObject = buildJsonObject {
        put("id", idOf(finding))
        put("name", SnippetSanitizer.escape(finding.ruleId))
        put("description", SnippetSanitizer.escape(finding.description.ifEmpty { finding.title }))
        put("engineId", ENGINE)
        put("cleanCodeAttribute", "TRUSTWORTHY")
        put(
            "impacts",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("softwareQuality", "SECURITY")
                        put("severity", impactOf(finding.gatingSeverity))
                    }
                )
            }
        )
    }

    private fun issueOf(finding: Finding): JsonObject = buildJsonObject {
        put("ruleId", idOf(finding))
        put(
            "primaryLocation",
            buildJsonObject {
                put("message", SnippetSanitizer.escape(finding.title))
                put("filePath", ReportFacts.filePathOf(finding))
                put("textRange", buildJsonObject { put("startLine", ReportFacts.lineOf(finding)) })
            }
        )
    }

    private fun impactOf(severity: Severity): String = when (severity) {
        Severity.CRITICAL -> "BLOCKER"
        Severity.HIGH -> "HIGH"
        Severity.MEDIUM -> "MEDIUM"
        Severity.LOW -> "LOW"
        Severity.UNKNOWN -> "INFO"
    }
}
