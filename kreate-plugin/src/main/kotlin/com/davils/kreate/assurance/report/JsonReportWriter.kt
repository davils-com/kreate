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

import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingJson
import com.davils.kreate.assurance.policy.Verdict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SCHEMA_VERSION: Int = 1

private const val TOOL_NAME: String = "kreate"

private val PRETTY_JSON: Json = Json { prettyPrint = true }

internal object JsonReportWriter {
    fun render(evaluation: AssuranceEvaluation): String = pretty(
        buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("tool", toolOf(evaluation))
            put("generatedAt", evaluation.generatedAt.toString())
            put("project", evaluation.project)
            put("result", resultOf(evaluation.verdict))
            put("sources", sourcesOf(evaluation.documents))
            put("findings", findingsOf(evaluation))
            put("suppressed", evaluation.outcome.suppressed.size)
            put("baselined", evaluation.outcome.baselined.size)
        }
    )

    fun renderResult(verdict: Verdict): String = pretty(resultOf(verdict))

    private fun pretty(json: JsonElement): String = PRETTY_JSON.encodeToString(JsonElement.serializer(), json) + "\n"

    private fun toolOf(evaluation: AssuranceEvaluation): JsonObject = buildJsonObject {
        put("name", TOOL_NAME)
        put("version", evaluation.toolVersion)
    }

    private fun resultOf(verdict: Verdict): JsonObject = buildJsonObject {
        put("status", verdict.status.name)
        put("exitCode", verdict.status.exitCode)
        put("blockingFindings", verdict.blockingFindings)
        put("staleBaselineEntries", verdict.staleEntries)
        put("incompleteSources", textArray(verdict.incompleteSources))
    }

    private fun textArray(values: List<String>): JsonArray = buildJsonArray {
        values.forEach { value -> add(JsonPrimitive(value)) }
    }

    private fun sourcesOf(documents: List<FindingDocument>): JsonArray = buildJsonArray {
        documents.forEach { document -> add(sourceOf(document)) }
    }

    private fun sourceOf(document: FindingDocument): JsonObject = buildJsonObject {
        put("source", document.source.id)
        put("status", document.status.name)
        put("evaluatedAt", document.evaluatedAt.toString())
        document.reason?.let { reason -> put("reason", reason) }
    }

    private fun findingsOf(evaluation: AssuranceEvaluation): JsonArray = buildJsonArray {
        evaluation.outcome.kept.forEach { finding -> add(FindingJson.write(finding)) }
    }
}
