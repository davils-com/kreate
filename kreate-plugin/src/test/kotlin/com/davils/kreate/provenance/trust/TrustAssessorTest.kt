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
package com.davils.kreate.provenance.trust

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.provenance.depsdev.PackageHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

private val NOW: Instant = Instant.parse("2026-11-07T12:00:00Z")

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private val THRESHOLDS: TrustThresholds = TrustThresholds(7, 30, 24, 4.0)

private fun daysAgo(days: Long): Instant = NOW.minus(Duration.ofDays(days))

private fun evidence(first: Instant, published: Instant = first, scorecard: Double? = null): TrustEvidence {
    val history = PackageHistory(mapOf("0.1.0" to first, "1.0.0" to published))
    return TrustEvidence(MODULE, history, "github.com/example/lib", scorecard)
}

private fun findingsOf(evidence: TrustEvidence): List<Finding> = TrustAssessor(THRESHOLDS, NOW).assess(evidence)

private fun rulesOf(evidence: TrustEvidence): List<String> = findingsOf(evidence).map { finding -> finding.ruleId }

class TrustAssessorTest : FunSpec({
    context("a module") {
        test("with a long history and a sound scorecard is trusted") {
            rulesOf(evidence(daysAgo(900), daysAgo(60), scorecard = 7.0)).shouldBeEmpty()
        }

        test("first published days ago is fresh") {
            val finding = findingsOf(evidence(daysAgo(3), daysAgo(2))).single()

            finding.ruleId shouldBe "FRESH_PACKAGE"
            finding.severity shouldBe Severity.CRITICAL
            finding.attributes["ageDays"] shouldBe "3"
        }

        test("first published weeks ago is young") {
            rulesOf(evidence(daysAgo(20), daysAgo(10))) shouldBe listOf("YOUNG_PACKAGE")
        }

        test("whose version was published hours ago is a fresh release") {
            val published = NOW.minus(Duration.ofHours(5))

            rulesOf(evidence(daysAgo(900), published)) shouldBe listOf("FRESH_RELEASE")
        }

        test("whose repository scores low is reported with the score") {
            val finding = findingsOf(evidence(daysAgo(900), daysAgo(90), 2.5)).single()

            finding.ruleId shouldBe "LOW_SCORECARD"
            finding.attributes["scorecard"] shouldBe "2.5"
        }

        test("without a scorecard is not reported, because missing data is no risk") {
            rulesOf(evidence(daysAgo(900), daysAgo(90))).shouldBeEmpty()
        }
    }

    context("missing data") {
        test("about the module is reported, without blocking") {
            val finding = findingsOf(TrustEvidence(MODULE, null)).single()

            finding.ruleId shouldBe "TRUST_DATA_UNAVAILABLE"
            (REPORT_ONLY_ATTRIBUTE in finding.attributes) shouldBe true
        }

        test("about the version still judges the age of the module") {
            val history = PackageHistory(mapOf("0.1.0" to daysAgo(2)))

            rulesOf(TrustEvidence(MODULE, history)) shouldBe listOf("FRESH_PACKAGE", "TRUST_DATA_UNAVAILABLE")
        }
    }
})
