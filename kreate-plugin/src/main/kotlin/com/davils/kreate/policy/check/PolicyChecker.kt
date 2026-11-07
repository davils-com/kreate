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
package com.davils.kreate.policy.check

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.governance.policy.matchesModule
import com.davils.kreate.policy.model.AssurancePolicy
import com.davils.kreate.policy.model.PolicyFormatException
import java.time.LocalDate

private const val UNSET: String = "(unset)"

internal class PolicyChecker(private val policy: AssurancePolicy) {
    fun check(input: PolicyInput): List<Finding> {
        validate(input.values)
        val values = input.values
        val required = policy.require.mapNotNull { (key, expected) -> requiredOf(key, expected, values[key]) }
        val minimums = policy.minimum.mapNotNull { (key, limit) -> minimumOf(key, limit, values[key]) }
        val ignores = input.ignoreRules.flatMap { rule -> ignoreFindingsOf(rule, input.today) }
        return required + minimums + ignores + deniedOf(input.modules)
    }

    private fun validate(values: Map<String, String>) {
        val named = policy.require.keys + policy.minimum.keys
        val unknown = named - values.keys
        if (unknown.isNotEmpty()) {
            throw PolicyFormatException("The policy ${policy.name} names unknown options: ${unknown.sorted()}.")
        }
        val unordered = policy.minimum.keys - Strictness.OF_OPTION.keys
        if (unordered.isNotEmpty()) {
            val names = unordered.sorted()
            throw PolicyFormatException("The policy ${policy.name} sets a minimum for unordered options: $names.")
        }
    }

    private fun requiredOf(key: String, expected: String, actual: String?): Finding? {
        if (actual == expected) return null
        val title = "$key is ${shown(actual)}, the policy ${policy.name} requires $expected"
        return finding("POLICY_REQUIRED_VALUE", title, mapOf("option" to key, "required" to expected))
    }

    private fun minimumOf(key: String, limit: String, actual: String?): Finding? {
        val order = Strictness.OF_OPTION.getValue(key)
        val strictEnough = actual?.let { value -> order.isAtLeastAsStrict(value, limit) }
        if (strictEnough == true) return null
        val title = "$key is ${shown(actual)}, weaker than $limit, the weakest the policy ${policy.name} allows"
        return finding("POLICY_BELOW_MINIMUM", title, mapOf("option" to key, "minimum" to limit))
    }

    private fun ignoreFindingsOf(rule: IgnoreRuleInput, today: LocalDate): List<Finding> {
        val expires = rule.expires?.let(LocalDate::parse)
        val attributes = mapOf("rule" to rule.id)
        if (expires == null && policy.ignoreRulesNeedExpiry) {
            return listOf(finding("POLICY_IGNORE_WITHOUT_EXPIRY", "ignore(\"${rule.id}\") has no expiry", attributes))
        }
        val maxDays = policy.maxIgnoreDays ?: return emptyList()
        val latest = today.plusDays(maxDays.toLong())
        val tooLong = expires == null || expires.isAfter(latest)
        if (!tooLong) return emptyList()
        val title = "ignore(\"${rule.id}\") lasts beyond $latest, more than $maxDays days"
        return listOf(finding("POLICY_IGNORE_TOO_LONG", title, attributes))
    }

    private fun deniedOf(modules: List<ModuleCoordinate>): List<Finding> {
        val byModule = modules.groupBy { module -> module.module }
        return byModule.values.flatMap { versions -> deniedVersionsOf(versions) }
    }

    private fun deniedVersionsOf(versions: List<ModuleCoordinate>): List<Finding> {
        val module = versions.first()
        val matching = policy.denied.filterKeys { pattern -> matchesModule(pattern, module.group, module.name) }
        val distinct = versions.map { version -> version.version }.distinct()
        val listed = distinct.sorted().joinToString()
        return matching.map { (pattern, reason) ->
            val title = "${module.module} ($listed) is denied by ${policy.name}: $reason"
            ruleFinding("POLICY_DENIED_MODULE", module, title, mapOf("pattern" to pattern, "policy" to policy.name))
        }
    }

    private fun shown(value: String?): String = value.orEmpty().ifEmpty { UNSET }

    private fun finding(rule: String, title: String, attributes: Map<String, String>): Finding =
        ruleFinding(rule, null, title, attributes + ("policy" to policy.name))
}
