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

import com.davils.kreate.assurance.task.IgnoreRuleInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val SOUND: Map<String, String> = mapOf(
    "reportOnly" to "false",
    "minSeverity" to "UNKNOWN",
    "trivy.enabled" to "true",
    "trivy.useInstalled" to "false",
    "advisories.enabled" to "true",
    "confusion.checkPublicRepository" to "false",
    "baseline.maxSeverity" to "MEDIUM"
)

private val TODAY: LocalDate = LocalDate.parse("2026-11-07")

private fun rulesOf(
    values: Map<String, String> = SOUND,
    rules: List<IgnoreRuleInput> = emptyList(),
    urls: Map<String, String> = emptyMap(),
    baselineExists: Boolean = false
): List<String> {
    val findings = ConfigLinter.lint(ConfigLintInput(values, rules, urls, baselineExists, TODAY))
    return findings.map { finding -> finding.ruleId }
}

class ConfigLinterTest : FunSpec({
    context("a sound configuration") {
        test("passes") {
            rulesOf().shouldBeEmpty()
        }
    }

    context("the gate") {
        test("that never fails is reported") {
            rulesOf(SOUND + ("reportOnly" to "true")) shouldBe listOf("CONFIG_GATE_NOT_FAILING")
        }

        test("that drops findings is reported") {
            rulesOf(SOUND + ("minSeverity" to "HIGH")) shouldBe listOf("CONFIG_MIN_SEVERITY")
        }

        test("without any source is reported") {
            val nothing = SOUND + ("trivy.enabled" to "false") + ("advisories.enabled" to "false")

            rulesOf(nothing) shouldContainExactlyInAnyOrder listOf("CONFIG_TRIVY_DISABLED", "CONFIG_NO_SOURCES")
        }
    }

    context("ignore rules") {
        test("without expiry and expired ones are reported") {
            val rules = listOf(
                IgnoreRuleInput("GHSA-1", "Reviewed.", null, null),
                IgnoreRuleInput("GHSA-2", "Reviewed.", null, "2026-01-01"),
                IgnoreRuleInput("GHSA-3", "Reviewed.", null, "2027-01-01")
            )

            rulesOf(rules = rules) shouldBe listOf("CONFIG_IGNORE_WITHOUT_EXPIRY", "CONFIG_IGNORE_EXPIRED")
        }
    }

    context("other settings") {
        test("plain HTTP is reported") {
            val urls = mapOf(
                "kreate.network.osvUrl" to "HTTP://osv.example",
                "kreate.network.depsDevUrl" to "http://127.0.0.1:8080"
            )

            rulesOf(urls = urls) shouldBe listOf("CONFIG_INSECURE_URL")
        }

        test("an installed Trivy is reported") {
            rulesOf(SOUND + ("trivy.useInstalled" to "true")) shouldBe listOf("CONFIG_UNPINNED_TRIVY")
        }

        test("a baseline without a limit is reported when there is one") {
            val unlimited = SOUND + ("baseline.maxSeverity" to "")

            rulesOf(unlimited, baselineExists = true) shouldBe listOf("CONFIG_BASELINE_UNBOUNDED")
            rulesOf(unlimited, baselineExists = false).shouldBeEmpty()
        }

        test("a public lookup of internal modules is reported") {
            rulesOf(SOUND + ("confusion.checkPublicRepository" to "true")) shouldBe listOf("CONFIG_PUBLIC_LOOKUP")
        }
    }

    context("the renderer") {
        test("marks what differs from the defaults") {
            val text = ConfigRenderer.render(mapOf("a" to "1", "bb" to ""), mapOf("a" to "2", "bb" to ""))

            text shouldBe "* a  = 1\n  bb = (unset)\n"
        }
    }
})
