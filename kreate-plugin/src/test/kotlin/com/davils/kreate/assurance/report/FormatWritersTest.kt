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
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.policy.FindingFilter
import com.davils.kreate.assurance.policy.VerdictRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.w3c.dom.Element
import java.time.Instant
import java.time.LocalDate
import javax.xml.parsers.DocumentBuilderFactory

private const val HOSTILE: String = "<script>alert('x')</script> & \"quoted\" | `code`"

private val FINDINGS: List<Finding> = listOf(
    Finding("GHSA-1", FindingSource.OSV, Severity.HIGH, HOSTILE, "Details <b>", module = "a:b", version = "1"),
    Finding(
        "RAW_SOCKET",
        FindingSource.INSPECTION,
        Severity.MEDIUM,
        "Opens a socket",
        location = SourceLocation("src/A.kt", 12)
    )
)

private fun evaluationOf(findings: List<Finding> = FINDINGS): AssuranceEvaluation {
    val documents = listOf(
        FindingDocument(FindingSource.OSV, DocumentStatus.COMPLETE, Instant.EPOCH, findings.take(1)),
        FindingDocument(FindingSource.INSPECTION, DocumentStatus.COMPLETE, Instant.EPOCH, findings.drop(1)),
        FindingDocument(FindingSource.TRUST, DocumentStatus.INCOMPLETE, Instant.EPOCH, reason = "offline")
    )
    val filter = FindingFilter(Severity.UNKNOWN, emptyList(), LocalDate.EPOCH, BaselineMatcher(emptyList(), null))
    val outcome = filter.apply(findings, setOf(FindingSource.OSV, FindingSource.INSPECTION))
    val verdict = VerdictRules(Severity.HIGH).decide(documents, outcome)
    return AssuranceEvaluation(":app", "4.0.0", Instant.EPOCH, documents, outcome, verdict)
}

private fun parsedXml(text: String): Element {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return builder.parse(text.byteInputStream()).documentElement
}

class FormatWritersTest : FunSpec({
    val evaluation = evaluationOf()

    context("the HTML dashboard") {
        val html = HtmlReportWriter.render(evaluation)

        test("escapes everything that comes from findings") {
            html shouldNotContain "<script>alert"
            html shouldContain "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"
        }

        test("allows only its own style and script, by hash") {
            val style = html.substringAfter("<style>").substringBefore("</style>")
            val script = html.substringAfter("<script>").substringBefore("</script>")

            html shouldContain "style-src '${HtmlDashboard.hashOf(style)}'"
            html shouldContain "script-src '${HtmlDashboard.hashOf(script)}'"
            html shouldContain "default-src 'none'"
        }

        test("shows the status, the counts and the incomplete source") {
            html shouldContain "status INCOMPLETE"
            html shouldContain "offline"
            html shouldContain "data-severity=\"HIGH\""
        }
    }

    context("the XML formats") {
        test("JUnit is well formed, with a suite per source and an error for the incomplete one") {
            val root = parsedXml(JunitReportWriter.render(evaluation))

            root.getElementsByTagName("testsuite").length shouldBe 3
            root.getElementsByTagName("failure").length shouldBe 2
            root.getElementsByTagName("error").length shouldBe 1
        }

        test("Checkstyle is well formed and groups by file") {
            val root = parsedXml(CheckstyleReportWriter.render(evaluation))

            root.getElementsByTagName("file").length shouldBe 2
            val first = root.getElementsByTagName("error").item(0) as Element
            first.getAttribute("line") shouldBe "1"
        }
    }

    context("the JSON formats") {
        test("GitLab lists every finding with a fingerprint and a mapped severity") {
            val issues = Json.parseToJsonElement(GitLabReportWriter.render(evaluation)) as JsonArray

            issues.size shouldBe 2
            issues.toString() shouldContain "\"severity\":\"critical\""
            issues.toString() shouldContain "\"begin\":12"
        }

        test("SonarQube names a rule for every issue") {
            val report = Json.parseToJsonElement(SonarQubeReportWriter.render(evaluation)) as JsonObject

            (report["rules"] as JsonArray).size shouldBe 2
            report.toString() shouldContain "\"ruleId\":\"osv.GHSA-1\""
        }
    }

    context("Markdown") {
        test("escapes table syntax and summarises") {
            val markdown = MarkdownReportWriter.render(evaluation)

            markdown shouldContain "## Kreate assurance: INCOMPLETE"
            markdown shouldContain "\\|"
            markdown shouldContain "- Incomplete: trust"
        }

        test("says so when nothing was found") {
            MarkdownReportWriter.render(evaluationOf(emptyList())) shouldContain "No findings."
        }
    }
})
