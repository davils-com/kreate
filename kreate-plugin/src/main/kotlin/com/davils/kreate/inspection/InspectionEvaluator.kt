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
package com.davils.kreate.inspection

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.rule.RuleCatalog
import com.davils.kreate.assurance.rule.RuleInfo
import com.davils.kreate.inspection.scan.Hit
import com.davils.kreate.inspection.scan.RiskScorer
import com.davils.kreate.inspection.scan.hostOf

private const val URL_RULE: String = "HARDCODED_URL"

private const val REPOSITORY_RULE: String = "POM_REPOSITORY"

private const val SHADOWING_RULE: String = "SHADOWED_PLATFORM_CLASS"

private const val RISK_RULE: String = "RISK_SCORE"

private val DEFAULT_TRUSTED_HOSTS: Set<String> = setOf(
    "apache.org", "w3.org", "w3c.org", "xml.org", "oracle.com", "sun.com", "java.net", "java.com", "jcp.org",
    "github.com", "githubusercontent.com", "gradle.org", "kotlinlang.org", "jetbrains.com", "jetbrains.org",
    "json-schema.org", "opensource.org", "eclipse.org", "mozilla.org", "maven.org", "sonatype.org",
    "example.com", "example.org", "example.net", "localhost", "127.0.0.1", "0.0.0.0",
    "sonatype.com", "kotl.in", "javax.xml.xmlconstants"
)

private val PLATFORM_GROUPS: List<String> =
    listOf("org.jetbrains.kotlin", "org.jetbrains.kotlinx", "org.gradle", "dev.gradleplugins")

internal class InspectionEvaluator(private val settings: InspectionSettings) {
    fun evaluate(module: InspectedModule): List<Finding> {
        val relevant = module.hits.filterNot { hit -> isExpected(hit, module) }
        val groups = relevant.groupBy { hit -> hit.rule to hit.detail }
        val findings = groups.values.map { group -> findingOf(group, module) }
        val rules = relevant.map { hit -> RuleCatalog.require(hit.rule) }
        val score = RiskScorer.scoreOf(rules)
        if (score < settings.failScore) return findings
        return findings + riskFindingOf(score, module, rules)
    }

    private fun isExpected(hit: Hit, module: InspectedModule): Boolean {
        if (hit.rule == URL_RULE) return isTrusted(hit.detail.orEmpty())
        if (hit.rule == REPOSITORY_RULE) return isTrusted(hostOf(hit.detail.orEmpty()).orEmpty())
        if (hit.rule == SHADOWING_RULE) return PLATFORM_GROUPS.any { group -> belongsTo(module, group) }
        return false
    }

    private fun belongsTo(module: InspectedModule, group: String): Boolean {
        val moduleGroup = module.coordinate.group
        return moduleGroup == group || moduleGroup.startsWith("$group.")
    }

    private fun isTrusted(host: String): Boolean {
        val trusted = DEFAULT_TRUSTED_HOSTS + settings.trustedHosts
        return trusted.any { candidate -> host == candidate || host.endsWith(".$candidate") }
    }

    private fun findingOf(group: List<Hit>, module: InspectedModule): Finding {
        val first = group.first()
        val rule = RuleCatalog.require(first.rule)
        return baseFinding(rule, module).copy(
            location = SourceLocation(first.path),
            attributes = attributesOf(group, first)
        )
    }

    private fun attributesOf(group: List<Hit>, first: Hit): Map<String, String> {
        val detail = first.detail?.let { value -> mapOf("detail" to value) }.orEmpty()
        return detail + mapOf("occurrences" to group.size.toString(), REPORT_ONLY_ATTRIBUTE to true.toString())
    }

    private fun riskFindingOf(score: Int, module: InspectedModule, rules: List<RuleInfo>): Finding {
        val ids = rules.map { rule -> rule.id }
        val names = ids.distinct().sorted()
        val risk = baseFinding(RuleCatalog.require(RISK_RULE), module)
        val attributes = mapOf("score" to score.toString(), "rules" to names.joinToString())
        if (settings.failOnRisk) return risk.copy(attributes = attributes)
        return risk.copy(attributes = attributes + (REPORT_ONLY_ATTRIBUTE to true.toString()))
    }

    private fun baseFinding(rule: RuleInfo, module: InspectedModule): Finding = Finding(
        ruleId = rule.id,
        source = rule.source,
        severity = rule.severity,
        title = rule.summary,
        description = rule.explanation,
        module = module.coordinate.module,
        version = module.coordinate.version
    )
}
