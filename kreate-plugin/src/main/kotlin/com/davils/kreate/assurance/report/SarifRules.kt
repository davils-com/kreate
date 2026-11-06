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
import com.davils.kreate.assurance.finding.FindingSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val SECURITY_SEVERITY_CRITICAL: String = "9.5"

private const val SECURITY_SEVERITY_HIGH: String = "8.0"

private const val SECURITY_SEVERITY_MEDIUM: String = "5.5"

private const val SECURITY_SEVERITY_LOW: String = "2.0"

private const val SECURITY_SEVERITY_UNKNOWN: String = "1.0"

private val MAINTAINABILITY_SOURCES: Set<FindingSource> = setOf(FindingSource.QUALITY, FindingSource.CONFIG)

internal object SarifRules {
    fun textOf(text: String): JsonObject = buildJsonObject { put("text", text) }

    fun ruleOf(finding: Finding): JsonObject = buildJsonObject {
        put("id", finding.ruleId)
        put("name", finding.ruleId)
        put("shortDescription", textOf(finding.title))
        finding.url?.let { url -> put("helpUri", url) }
        put("properties", rulePropertiesOf(finding))
    }

    private fun rulePropertiesOf(finding: Finding): JsonObject = buildJsonObject {
        val isMaintainability = finding.source in MAINTAINABILITY_SOURCES
        val category = categoryOf(isMaintainability)
        put("tags", textArray(listOf(category, finding.source.id)))
        if (!isMaintainability) put("security-severity", securitySeverityOf(finding.gatingSeverity))
    }

    private fun categoryOf(isMaintainability: Boolean): String {
        if (isMaintainability) return "maintainability"
        return "security"
    }

    private fun securitySeverityOf(severity: Severity): String = when (severity) {
        Severity.CRITICAL -> SECURITY_SEVERITY_CRITICAL
        Severity.HIGH -> SECURITY_SEVERITY_HIGH
        Severity.MEDIUM -> SECURITY_SEVERITY_MEDIUM
        Severity.LOW -> SECURITY_SEVERITY_LOW
        Severity.UNKNOWN -> SECURITY_SEVERITY_UNKNOWN
    }

    private fun textArray(values: List<String>): JsonArray = buildJsonArray {
        values.forEach { value -> add(JsonPrimitive(value)) }
    }
}
