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
package com.davils.kreate.assurance.config

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.assurance.task.IgnoreRuleInput
import java.time.LocalDate

private const val TRUE: String = "true"

private val LOOPBACK_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "[::1]")

private const val TRIVY_ENABLED: String = "trivy.enabled"

private val SOURCE_SWITCHES: List<String> = listOf(
    TRIVY_ENABLED,
    "advisories.enabled",
    "inspection.enabled",
    "trust.enabled",
    "confusion.enabled",
    "dependencyPolicy.enabled",
    "workspace.enabled"
)

internal object ConfigLinter {
    fun lint(input: ConfigLintInput): List<Finding> {
        val values = input.values
        val gate = gateOf(values)
        val ignores = input.ignoreRules.mapNotNull { rule -> ignoreFindingOf(rule, input.today) }
        return gate + ignores + urlsOf(input.urls) + coverageOf(values) + baselineOf(values, input.baselineExists)
    }

    private fun gateOf(values: Map<String, String>): List<Finding> {
        val findings = mutableListOf<Finding>()
        if (values["reportOnly"] == TRUE) findings += finding("CONFIG_GATE_NOT_FAILING", "reportOnly is true")
        val minimum = values["minSeverity"]
        if (minimum != null && minimum != "UNKNOWN") {
            findings += finding("CONFIG_MIN_SEVERITY", "Findings below $minimum are dropped")
        }
        if (values["confusion.checkPublicRepository"] == TRUE) {
            val repository = values["confusion.publicRepository"].orEmpty()
            findings += finding("CONFIG_PUBLIC_LOOKUP", "Internal modules are looked up in $repository")
        }
        return findings
    }

    private fun ignoreFindingOf(rule: IgnoreRuleInput, today: LocalDate): Finding? {
        val expires = rule.expires?.let(LocalDate::parse)
            ?: return finding("CONFIG_IGNORE_WITHOUT_EXPIRY", "ignore(\"${rule.id}\") never expires")
        if (!today.isAfter(expires)) return null
        return finding("CONFIG_IGNORE_EXPIRED", "ignore(\"${rule.id}\") expired on $expires")
    }

    private fun urlsOf(urls: Map<String, String>): List<Finding> {
        val insecure = urls.filterValues { url -> isPlainHttp(url) }
        return insecure.map { (key, url) -> finding("CONFIG_INSECURE_URL", "$key is $url") }
    }

    private fun isPlainHttp(url: String): Boolean {
        val normalized = url.trim().lowercase()
        if (!normalized.startsWith("http://")) return false
        val authority = normalized.removePrefix("http://").substringBefore('/')
        val host = authority.substringBeforeLast(':')
        return host !in LOOPBACK_HOSTS
    }

    private fun coverageOf(values: Map<String, String>): List<Finding> {
        val findings = mutableListOf<Finding>()
        val trivyOn = values[TRIVY_ENABLED] == TRUE
        if (!trivyOn) findings += finding("CONFIG_TRIVY_DISABLED", "kreate { trivy { enabled } } is false")
        if (trivyOn && values["trivy.useInstalled"] == TRUE) {
            findings += finding("CONFIG_UNPINNED_TRIVY", "trivy { useInstalled } is true")
        }
        if (SOURCE_SWITCHES.none { key -> values[key] == TRUE }) {
            findings += finding("CONFIG_NO_SOURCES", "No source of findings is enabled")
        }
        return findings
    }

    private fun baselineOf(values: Map<String, String>, baselineExists: Boolean): List<Finding> {
        if (!baselineExists || !values["baseline.maxSeverity"].isNullOrEmpty()) return emptyList()
        return listOf(finding("CONFIG_BASELINE_UNBOUNDED", "baseline { maxSeverity } is not set"))
    }

    private fun finding(rule: String, title: String): Finding = ruleFinding(rule, null, title)
}
