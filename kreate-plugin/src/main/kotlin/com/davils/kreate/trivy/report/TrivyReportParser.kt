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

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.Finding
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object TrivyReportParser {
    fun parse(report: String, pathOf: (String) -> String): List<Finding> {
        val results = resultsOf(parseReport(report))
        return results.flatMap { result -> findingsOf(result, pathOf) }
    }

    private fun parseReport(report: String): JsonElement = try {
        Json.parseToJsonElement(report)
    } catch (cause: SerializationException) {
        throw AssuranceInputException("Trivy wrote a report that is not valid JSON.", cause)
    }

    private fun resultsOf(report: JsonElement): List<JsonObject> {
        val document = report as? JsonObject ?: throw AssuranceInputException("The Trivy report is not an object.")
        val results = document["Results"] as? JsonArray ?: return emptyList()
        return results.filterIsInstance<JsonObject>()
    }

    private fun findingsOf(result: JsonObject, pathOf: (String) -> String): List<Finding> {
        val target = pathOf(result.text("Target").orEmpty())
        return TrivyFindings.vulnerabilities(result.objects("Vulnerabilities"), target) +
            TrivyFindings.licenses(result.objects("Licenses"), target) +
            TrivyFindings.secrets(result.objects("Secrets"), target) +
            TrivyFindings.misconfigurations(result.objects("Misconfigurations"), target)
    }
}
