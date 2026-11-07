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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val PRETTY_JSON: Json = Json { prettyPrint = true }

internal object GitLabReportWriter {
    fun render(evaluation: AssuranceEvaluation): String {
        val issues = JsonArray(evaluation.outcome.kept.map(::issueOf))
        return PRETTY_JSON.encodeToString(JsonElement.serializer(), issues) + "\n"
    }

    private fun issueOf(finding: Finding) = buildJsonObject {
        put("description", SnippetSanitizer.escape(finding.title))
        put("check_name", "${finding.source.id}/${finding.ruleId}")
        put("fingerprint", finding.fingerprint)
        put("severity", severityOf(finding.gatingSeverity))
        put(
            "location",
            buildJsonObject {
                put("path", ReportFacts.filePathOf(finding))
                put("lines", buildJsonObject { put("begin", ReportFacts.lineOf(finding)) })
            }
        )
    }

    private fun severityOf(severity: Severity): String = when (severity) {
        Severity.CRITICAL -> "blocker"
        Severity.HIGH -> "critical"
        Severity.MEDIUM -> "major"
        Severity.LOW -> "minor"
        Severity.UNKNOWN -> "info"
    }
}
