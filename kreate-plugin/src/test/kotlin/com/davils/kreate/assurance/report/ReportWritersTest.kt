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
package com.davils.kreate.assurance.report

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.finding
import com.davils.kreate.assurance.policy.FilterOutcome
import com.davils.kreate.assurance.policy.GateStatus
import com.davils.kreate.assurance.policy.IgnoreRule
import com.davils.kreate.assurance.policy.Verdict
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

private fun evaluationOf(
    findings: List<Finding>,
    status: GateStatus = GateStatus.FINDINGS,
    expired: List<IgnoreRule> = emptyList()
): AssuranceEvaluation = AssuranceEvaluation(
    project = ":sample",
    toolVersion = "4.0.0",
    generatedAt = Instant.parse("2026-11-06T12:00:00Z"),
    documents = listOf(FindingDocument(FindingSource.OSV, DocumentStatus.COMPLETE, Instant.EPOCH)),
    outcome = FilterOutcome(findings, emptyList(), emptyList(), expired, 0),
    verdict = Verdict(status, findings.size, emptyList(), 0)
)

private fun runOf(sarif: String): JsonObject {
    val document = Json.parseToJsonElement(sarif).jsonObject
    val runs = document.getValue("runs").jsonArray
    return runs.single().jsonObject
}

private fun resultsOf(run: JsonObject): JsonArray = run.getValue("results").jsonArray

private fun rulesOf(run: JsonObject): JsonArray {
    val tool = run.getValue("tool").jsonObject
    val driver = tool.getValue("driver").jsonObject
    return driver.getValue("rules").jsonArray
}

class ReportWritersTest : FunSpec({
    context("the JSON report") {
        test("carries the schema version, the result and the fingerprints") {
            val found = finding()

            val report = Json.parseToJsonElement(JsonReportWriter.render(evaluationOf(listOf(found)))).jsonObject

            report.getValue("schemaVersion").jsonPrimitive.content shouldBe "1"
            val result = report.getValue("result").jsonObject
            result.getValue("exitCode").jsonPrimitive.content shouldBe "1"
            val listed = report.getValue("findings").jsonArray.single().jsonObject
            listed.getValue("fingerprint").jsonPrimitive.content shouldBe found.fingerprint
        }
    }

    context("the SARIF report") {
        test("names its schema and version") {
            val report = SarifReportWriter.render(evaluationOf(emptyList()))

            report shouldContain "sarif-2.1.0.json"
            report shouldContain "\"2.1.0\""
        }

        test("lists a rule once, however often it fires") {
            val run = runOf(SarifReportWriter.render(evaluationOf(listOf(finding(), finding()))))

            rulesOf(run).size shouldBe 1
            resultsOf(run).size shouldBe 2
        }

        test("maps severities to SARIF levels") {
            val findings = listOf(
                finding(severity = Severity.CRITICAL),
                finding(severity = Severity.MEDIUM),
                finding(severity = Severity.LOW)
            )

            val results = resultsOf(runOf(SarifReportWriter.render(evaluationOf(findings))))

            val levels = results.map { result -> result.jsonObject.getValue("level").jsonPrimitive.content }
            levels shouldBe listOf("error", "warning", "note")
        }

        test("rates an unrated advisory by its gating severity") {
            val advisory = finding(source = FindingSource.OSV, severity = Severity.UNKNOWN)

            val results = resultsOf(runOf(SarifReportWriter.render(evaluationOf(listOf(advisory)))))

            results.single().jsonObject.getValue("level").jsonPrimitive.content shouldBe "error"
        }

        test("gives code scanning a fingerprint and a location") {
            val located = finding(path = "src/A.kt", line = 7)

            val rendered = SarifReportWriter.render(evaluationOf(listOf(located)))

            rendered shouldContain "kreate/v1"
            rendered shouldContain located.fingerprint
            rendered shouldContain "src/A.kt"
            rendered shouldContain "startLine"
        }

        test("marks a quality finding as maintainability, without a security severity") {
            val quality = finding(source = FindingSource.QUALITY)

            val rendered = SarifReportWriter.render(evaluationOf(listOf(quality)))

            rendered shouldContain "maintainability"
            rendered.contains("security-severity") shouldBe false
        }
    }

    context("the text report") {
        test("leads with the status and lists the most severe finding first") {
            val findings = listOf(finding(rule = "MINOR", severity = Severity.LOW), finding(rule = "MAJOR"))

            val report = TextReportWriter.render(evaluationOf(findings))

            report shouldContain "Status: FINDINGS"
            (report.indexOf("MAJOR") < report.indexOf("MINOR")) shouldBe true
        }

        test("mentions an ignore rule that expired") {
            val rule = IgnoreRule("OLD", "Accepted")

            TextReportWriter.render(evaluationOf(emptyList(), expired = listOf(rule))) shouldContain "OLD"
        }
    }
})
