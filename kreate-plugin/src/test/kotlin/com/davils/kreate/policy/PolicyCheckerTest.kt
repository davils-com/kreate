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
package com.davils.kreate.policy

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.policy.check.PolicyChecker
import com.davils.kreate.policy.check.PolicyInput
import com.davils.kreate.policy.check.Strictness
import com.davils.kreate.policy.model.AssurancePolicy
import com.davils.kreate.policy.model.AssurancePolicyJson
import com.davils.kreate.policy.model.AssurancePolicyWriter
import com.davils.kreate.policy.model.PolicyFormatException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

private val VALUES: Map<String, String> = mapOf(
    "enabled" to "true",
    "reportOnly" to "false",
    "failOn" to "HIGH",
    "maxAdvisoryAge" to "PT72H",
    "trust.freshReleaseHours" to "24",
    "inspection.failScore" to "30"
)

private val TODAY: LocalDate = LocalDate.parse("2026-11-07")

private fun violationsOf(
    policy: AssurancePolicy,
    values: Map<String, String> = VALUES,
    rules: List<IgnoreRuleInput> = emptyList(),
    modules: List<ModuleCoordinate> = emptyList()
): List<String> {
    val findings = PolicyChecker(policy).check(PolicyInput(values, rules, modules, TODAY))
    return findings.map { finding -> finding.ruleId }
}

class PolicyCheckerTest : FunSpec({
    context("required values") {
        val policy = AssurancePolicy("org", require = mapOf("enabled" to "true", "reportOnly" to "false"))

        test("pass when the build keeps them") {
            violationsOf(policy).shouldBeEmpty()
        }

        test("are violated by a different value") {
            violationsOf(policy, VALUES + ("reportOnly" to "true")) shouldBe listOf("POLICY_REQUIRED_VALUE")
        }

        test("name only options that exist") {
            val unknown = AssurancePolicy("org", require = mapOf("nonsense" to "true"))

            shouldThrow<PolicyFormatException> { violationsOf(unknown) }.message shouldContain "nonsense"
        }
    }

    context("minimum values") {
        test("accept the same or a stricter value") {
            val policy = AssurancePolicy(
                "org",
                minimum = mapOf("failOn" to "HIGH", "maxAdvisoryAge" to "P3D", "trust.freshReleaseHours" to "12")
            )

            violationsOf(policy).shouldBeEmpty()
            violationsOf(policy, VALUES + ("failOn" to "LOW")).shouldBeEmpty()
        }

        test("reject a weaker value") {
            val policy = AssurancePolicy("org", minimum = mapOf("failOn" to "MEDIUM", "inspection.failScore" to "20"))

            violationsOf(policy) shouldBe listOf("POLICY_BELOW_MINIMUM", "POLICY_BELOW_MINIMUM")
        }

        test("are only allowed for options with an order") {
            val unordered = AssurancePolicy("org", minimum = mapOf("reportOnly" to "false"))

            shouldThrow<PolicyFormatException> { violationsOf(unordered) }
        }
    }

    context("ignore rules") {
        val rules = listOf(
            IgnoreRuleInput("GHSA-1", "Reviewed.", null, null),
            IgnoreRuleInput("GHSA-2", "Reviewed.", null, "2027-12-31"),
            IgnoreRuleInput("GHSA-3", "Reviewed.", null, "2026-12-01")
        )

        test("need an expiry and may not last too long") {
            val policy = AssurancePolicy("org", ignoreRulesNeedExpiry = true, maxIgnoreDays = 90)

            val violations = violationsOf(policy, rules = rules)

            violations shouldBe listOf("POLICY_IGNORE_WITHOUT_EXPIRY", "POLICY_IGNORE_TOO_LONG")
        }
    }

    context("denied modules") {
        test("are reported once per module with all versions") {
            val policy = AssurancePolicy("org", denied = mapOf("log4j:log4j" to "End of life."))
            val modules = listOf(
                ModuleCoordinate("log4j", "log4j", "1.2.17"),
                ModuleCoordinate("log4j", "log4j", "1.2.14")
            )
            val findings = PolicyChecker(policy).check(PolicyInput(VALUES, emptyList(), modules, TODAY))

            val finding = findings.single()

            finding.title shouldBe "log4j:log4j (1.2.14, 1.2.17) is denied by org: End of life."
        }
    }

    context("the order of options") {
        test("knows which way is stricter") {
            Strictness.LESS_SEVERE.isAtLeastAsStrict("LOW", "HIGH") shouldBe true
            Strictness.LESS_SEVERE.isAtLeastAsStrict("CRITICAL", "HIGH") shouldBe false
            Strictness.SHORTER_DURATION.isAtLeastAsStrict("PT12H", "P1D") shouldBe true
            Strictness.HIGHER_NUMBER.isAtLeastAsStrict("3.5", "4") shouldBe false
            Strictness.LOWER_NUMBER.isAtLeastAsStrict("nonsense", "4") shouldBe null
        }
    }

    context("the policy file") {
        test("survives a round trip") {
            val policy = AssurancePolicy(
                name = "org",
                require = mapOf("enabled" to "true"),
                minimum = mapOf("failOn" to "HIGH"),
                forbidIgnoreOf = setOf(Severity.CRITICAL),
                ignoreRulesNeedExpiry = true,
                maxIgnoreDays = 90,
                denied = mapOf("log4j:log4j" to "End of life.")
            )

            AssurancePolicyJson.parse(AssurancePolicyWriter.render(policy)) shouldBe policy
        }

        listOf(
            "[]" to "no JSON object",
            """{"schemaVersion":1,"name":"o","surprise":1}""" to "unknown keys",
            """{"schemaVersion":2,"name":"o"}""" to "schemaVersion 1",
            """{"schemaVersion":1}""" to "needs a name",
            """{"schemaVersion":1,"name":"o","forbidIgnoreOf":["SEVERE"]}""" to "no severity",
            """{"schemaVersion":1,"name":"o","maxIgnoreDays":-1}""" to "must not be negative",
            """{"schemaVersion":1,"name":"o","require":[]}""" to "must be an object"
        ).forEach { (text, problem) ->
            test("refuses a policy that says '$problem'") {
                shouldThrow<PolicyFormatException> { AssurancePolicyJson.parse(text) }.message shouldContain problem
            }
        }
    }
})
