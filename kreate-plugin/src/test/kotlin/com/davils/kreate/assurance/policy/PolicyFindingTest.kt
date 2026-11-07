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
package com.davils.kreate.assurance.policy

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

private val VIOLATION: Finding =
    Finding("POLICY_REQUIRED_VALUE", FindingSource.POLICY, Severity.CRITICAL, "reportOnly is true")

private fun outcomeOf(minSeverity: Severity = Severity.UNKNOWN): FilterOutcome {
    val rules = listOf(IgnoreRule("POLICY_REQUIRED_VALUE", "We know better."))
    val filter = FindingFilter(minSeverity, rules, LocalDate.parse("2026-11-07"), BaselineMatcher(emptyList(), null))
    return filter.apply(listOf(VIOLATION), setOf(FindingSource.POLICY))
}

class PolicyFindingTest : FunSpec({
    context("a policy violation") {
        test("cannot be ignored or hidden by minSeverity") {
            outcomeOf(Severity.CRITICAL).kept.single().ruleId shouldBe "POLICY_REQUIRED_VALUE"
            outcomeOf().suppressed.size shouldBe 0
        }

        test("fails the gate even when it only reports") {
            val violations = listOf(VIOLATION)
            val document = FindingDocument(FindingSource.POLICY, DocumentStatus.COMPLETE, Instant.EPOCH, violations)

            val verdict = VerdictRules(Severity.CRITICAL, reportOnly = true).decide(listOf(document), outcomeOf())

            verdict.status shouldBe GateStatus.FINDINGS
        }
    }
})
