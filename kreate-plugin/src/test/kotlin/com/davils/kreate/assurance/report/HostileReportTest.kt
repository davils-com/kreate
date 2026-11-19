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

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.SourceLocation
import com.davils.kreate.assurance.policy.FilterOutcome
import com.davils.kreate.assurance.policy.GateStatus
import com.davils.kreate.assurance.policy.Verdict
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.time.Instant

private val hostileTexts: Arb<String> = Arb.list(Arb.element(HostileFragments.all), 0..16)
    .map { fragments -> fragments.joinToString("") }

private val writers: Map<String, (AssuranceEvaluation) -> String> = mapOf(
    "text" to TextReportWriter::render,
    "Markdown" to MarkdownReportWriter::render,
    "HTML" to HtmlReportWriter::render,
    "JUnit" to JunitReportWriter::render,
    "Checkstyle" to CheckstyleReportWriter::render,
    "SARIF" to SarifReportWriter::render,
    "JSON" to JsonReportWriter::render,
    "GitLab" to GitLabReportWriter::render,
    "SonarQube" to SonarQubeReportWriter::render
)

private fun hostileFinding(text: String): Finding = Finding(
    ruleId = "RULE",
    source = FindingSource.INSPECTION,
    severity = Severity.HIGH,
    title = SnippetSanitizer.escape("title $text"),
    description = SnippetSanitizer.escape("description $text"),
    location = SourceLocation("src/Main.kt", 1),
    module = "com.example:lib",
    snippet = SnippetSanitizer.sanitize("snippet $text")
)

private fun evaluationOf(finding: Finding): AssuranceEvaluation = AssuranceEvaluation(
    project = ":sample",
    toolVersion = "5.3.0",
    generatedAt = Instant.parse("2026-11-19T12:00:00Z"),
    documents = listOf(FindingDocument(FindingSource.INSPECTION, DocumentStatus.COMPLETE, Instant.EPOCH)),
    outcome = FilterOutcome(listOf(finding), emptyList(), emptyList(), emptyList(), 0),
    verdict = Verdict(GateStatus.FINDINGS, 1, emptyList(), 0)
)

private fun unsafeLinesOf(rendered: String): List<String> =
    rendered.lines().flatMap { line -> unsafeCodePointsIn(line, allowTabs = true) }

class HostileReportTest : FunSpec({
    context("the sanitizer") {
        test("escapes every hostile text to one safe line") {
            checkAll(hostileTexts) { text ->
                unsafeCodePointsIn(SnippetSanitizer.escape(text)).shouldBeEmpty()
                unsafeCodePointsIn(SnippetSanitizer.sanitize(text)).shouldBeEmpty()
            }
        }

        test("keeps XML and Markdown safe") {
            checkAll(hostileTexts) { text ->
                unsafeCodePointsIn(ReportEscaping.xml(text)).shouldBeEmpty()
                unsafeCodePointsIn(ReportEscaping.markdown(text)).shouldBeEmpty()
            }
        }
    }

    writers.forEach { (format, render) ->
        test("the $format report shows hostile findings safely") {
            checkAll(hostileTexts) { text ->
                unsafeLinesOf(render(evaluationOf(hostileFinding(text)))).shouldBeEmpty()
            }
        }
    }
})
