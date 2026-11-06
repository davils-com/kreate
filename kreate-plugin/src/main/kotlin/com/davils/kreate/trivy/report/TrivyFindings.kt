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

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.finding.parseSeverity
import com.davils.kreate.assurance.report.SnippetSanitizer
import kotlinx.serialization.json.JsonObject

private const val SEVERITY: String = "Severity"

private const val TITLE: String = "Title"

private const val DESCRIPTION: String = "Description"

private const val PRIMARY_URL: String = "PrimaryURL"

private const val PACKAGE_NAME: String = "PkgName"

private const val UNKNOWN_SEVERITY: String = "UNKNOWN"

private const val SECRET_RULE: String = "secret"

private const val MISCONFIGURATION_RULE: String = "misconfig"

private const val LICENSE_RULE_PREFIX: String = "LICENSE:"

internal object TrivyFindings {
    fun vulnerabilities(entries: List<JsonObject>, target: String): List<Finding> = entries.map { entry ->
        val id = entry.text("VulnerabilityID").orEmpty()
        Finding(
            ruleId = id,
            source = FindingSource.TRIVY,
            severity = severityOf(entry),
            title = readable(entry.text(TITLE) ?: id),
            description = readable(entry.text(DESCRIPTION).orEmpty()),
            location = SourceLocation(target),
            module = entry.text(PACKAGE_NAME),
            version = entry.text("InstalledVersion"),
            fixedVersion = entry.text("FixedVersion"),
            url = entry.text(PRIMARY_URL)
        )
    }

    fun licenses(entries: List<JsonObject>, target: String): List<Finding> = entries.map { entry ->
        val name = entry.text("Name").orEmpty()
        Finding(
            ruleId = LICENSE_RULE_PREFIX + name,
            source = FindingSource.TRIVY,
            severity = severityOf(entry),
            title = readable("Restricted license $name"),
            location = SourceLocation(entry.text("FilePath") ?: target),
            module = entry.text(PACKAGE_NAME),
            url = entry.text("Link")
        )
    }

    fun secrets(entries: List<JsonObject>, target: String): List<Finding> = entries.map { entry ->
        Finding(
            ruleId = entry.text("RuleID") ?: SECRET_RULE,
            source = FindingSource.TRIVY,
            severity = severityOf(entry),
            title = readable(entry.text(TITLE) ?: SECRET_RULE),
            location = SourceLocation(target, entry.number("StartLine")),
            snippet = entry.text("Match")?.let(SnippetSanitizer::sanitize)
        )
    }

    fun misconfigurations(entries: List<JsonObject>, target: String): List<Finding> = entries.map { entry ->
        val rule = entry.text("ID") ?: entry.text("AVDID") ?: MISCONFIGURATION_RULE
        Finding(
            ruleId = rule,
            source = FindingSource.TRIVY,
            severity = severityOf(entry),
            title = readable(entry.text(TITLE) ?: rule),
            description = readable(entry.text("Resolution") ?: entry.text(DESCRIPTION).orEmpty()),
            location = SourceLocation(target, causeLine(entry)),
            url = entry.text(PRIMARY_URL)
        )
    }

    private fun causeLine(entry: JsonObject): Int? {
        val cause = entry["CauseMetadata"] as? JsonObject ?: return null
        return cause.number("StartLine")
    }

    private fun severityOf(entry: JsonObject): Severity =
        parseSeverity(entry.text(SEVERITY) ?: UNKNOWN_SEVERITY)

    private fun readable(text: String): String = SnippetSanitizer.escape(text)
}
