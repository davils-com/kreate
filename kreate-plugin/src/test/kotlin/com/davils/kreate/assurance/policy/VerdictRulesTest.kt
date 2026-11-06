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
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.finding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant

private fun document(status: DocumentStatus): FindingDocument =
    FindingDocument(FindingSource.OSV, status, Instant.EPOCH)

private fun outcome(findings: List<Finding> = emptyList(), staleEntries: Int = 0): FilterOutcome =
    FilterOutcome(findings, emptyList(), emptyList(), emptyList(), staleEntries)

class VerdictRulesTest : FunSpec({
    val complete = listOf(document(DocumentStatus.COMPLETE))

    context("a verdict") {
        test("passes without findings") {
            VerdictRules(Severity.HIGH).decide(complete, outcome()).status shouldBe GateStatus.PASSED
        }

        test("passes below the threshold") {
            val low = outcome(listOf(finding(severity = Severity.LOW)))

            VerdictRules(Severity.HIGH).decide(complete, low).status shouldBe GateStatus.PASSED
        }

        test("fails at the threshold and counts what blocks") {
            val findings = outcome(listOf(finding(severity = Severity.HIGH), finding(severity = Severity.LOW)))

            val verdict = VerdictRules(Severity.HIGH).decide(complete, findings)

            verdict.status shouldBe GateStatus.FINDINGS
            verdict.blockingFindings shouldBe 1
        }

        test("fails on an advisory without a rating, whatever its label says") {
            val advisory = finding(source = FindingSource.TRIVY, severity = Severity.UNKNOWN)

            VerdictRules(Severity.HIGH).decide(complete, outcome(listOf(advisory))).status shouldBe GateStatus.FINDINGS
        }

        test("hides findings when only reporting") {
            val high = outcome(listOf(finding()))

            VerdictRules(Severity.HIGH, reportOnly = true).decide(complete, high).status shouldBe GateStatus.PASSED
        }

        test("never reports an incomplete check as clean, not even when only reporting") {
            val documents = listOf(document(DocumentStatus.INCOMPLETE))

            val verdict = VerdictRules(Severity.HIGH, reportOnly = true).decide(documents, outcome())

            verdict.status shouldBe GateStatus.INCOMPLETE
            verdict.incompleteSources shouldContainExactly listOf("osv")
        }

        test("puts incomplete above findings") {
            val documents = listOf(document(DocumentStatus.INCOMPLETE))

            VerdictRules(Severity.HIGH).decide(documents, outcome(listOf(finding()))).status shouldBe
                GateStatus.INCOMPLETE
        }

        test("does not take a skipped source for an incomplete one") {
            val documents = listOf(document(DocumentStatus.SKIPPED))

            VerdictRules(Severity.HIGH).decide(documents, outcome()).status shouldBe GateStatus.PASSED
        }

        test("fails on stale baseline entries only when asked to") {
            val stale = outcome(staleEntries = 2)

            VerdictRules(Severity.HIGH).decide(complete, stale).status shouldBe GateStatus.PASSED
            VerdictRules(Severity.HIGH, failOnStale = true).decide(complete, stale).status shouldBe GateStatus.FINDINGS
        }
    }

    context("exit codes") {
        test("match what CI scripts rely on") {
            GateStatus.entries.map { status -> status.exitCode } shouldContainExactly listOf(0, 1, 65, 69)
        }
    }
})
