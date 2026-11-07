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
package com.davils.kreate.assurance.aggregate

import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.policy.FindingFilter
import com.davils.kreate.assurance.policy.GateStatus
import com.davils.kreate.assurance.policy.VerdictRules
import com.davils.kreate.assurance.report.AssuranceEvaluation
import com.davils.kreate.assurance.report.JsonReportWriter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant
import java.time.LocalDate

private fun summaryOf(project: String, status: GateStatus, blocking: Int = 0) =
    ProjectSummary(project, status, blocking, emptyList(), emptyList())

class AggregateTest : FunSpec({
    context("a project summary") {
        test("is read back from the JSON report") {
            val finding = Finding("GHSA-1", FindingSource.OSV, Severity.HIGH, "A flaw", module = "a:b", version = "1")
            val document = FindingDocument(FindingSource.OSV, DocumentStatus.COMPLETE, Instant.EPOCH, listOf(finding))
            val documents = listOf(document)
            val baseline = BaselineMatcher(emptyList(), null)
            val filter = FindingFilter(Severity.UNKNOWN, emptyList(), LocalDate.EPOCH, baseline)
            val outcome = filter.apply(listOf(finding), setOf(FindingSource.OSV))
            val verdict = VerdictRules(Severity.HIGH).decide(documents, outcome)
            val evaluation = AssuranceEvaluation(":app", "4.0.0", Instant.EPOCH, documents, outcome, verdict)

            val summary = ProjectSummaryJson.parse(JsonReportWriter.render(evaluation), "summary.json")

            summary shouldBe ProjectSummary(":app", GateStatus.FINDINGS, 1, emptyList(), listOf(finding))
        }

        test("that is no report is none") {
            ProjectSummaryJson.parse("{}", "x").shouldBeNull()
            ProjectSummaryJson.parse("nonsense", "x").shouldBeNull()
        }
    }

    context("the aggregate") {
        test("takes the worst status and sums the blocking findings") {
            val report = AggregateReport(
                listOf(summaryOf(":a", GateStatus.PASSED), summaryOf(":b", GateStatus.FINDINGS, 2)),
                emptyList(),
                Instant.EPOCH
            )

            report.status shouldBe GateStatus.FINDINGS
            report.blockingFindings shouldBe 2
            val mixed = listOf(summaryOf(":a", GateStatus.FINDINGS), summaryOf(":b", GateStatus.INCOMPLETE))
            AggregateReport(mixed, emptyList(), Instant.EPOCH).status shouldBe GateStatus.INCOMPLETE
        }

        test("renders the projects and the unchecked ones") {
            val report = AggregateReport(listOf(summaryOf(":a", GateStatus.PASSED)), listOf(":legacy"), Instant.EPOCH)

            AggregateWriters.render(report, ReportFormat.TEXT).orEmpty() shouldContain "NOT CHECKED :legacy"
            AggregateWriters.render(report, ReportFormat.MARKDOWN).orEmpty() shouldContain "| :a | PASSED | 0 | 0 |"
            AggregateWriters.render(report, ReportFormat.HTML).orEmpty() shouldContain "<li>:legacy</li>"
            AggregateWriters.render(report, ReportFormat.JSON).orEmpty() shouldContain "\"unchecked\": ["
            AggregateWriters.render(report, ReportFormat.SARIF).shouldBeNull()
        }
    }
})
