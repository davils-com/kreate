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
package com.davils.kreate.assurance.finding

import com.davils.kreate.assurance.Severity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant

private val EVALUATED_AT: Instant = Instant.parse("2026-11-06T12:00:00Z")

class FindingDocumentJsonTest : FunSpec({
    context("a finding document") {
        test("survives a round trip with everything set") {
            val finding = Finding(
                ruleId = "GHSA-1",
                source = FindingSource.OSV,
                severity = Severity.CRITICAL,
                title = "Title",
                description = "Description",
                location = SourceLocation("a.kt", 7),
                module = "com.example:lib",
                version = "1.0.0",
                fixedVersion = "1.0.1",
                aliases = listOf("CVE-1"),
                url = "https://example.com",
                snippet = "code",
                attributes = mapOf("key" to "value")
            )
            val document = FindingDocument(
                source = FindingSource.OSV,
                status = DocumentStatus.INCOMPLETE,
                evaluatedAt = EVALUATED_AT,
                findings = listOf(finding),
                reason = "Offline",
                dataAsOf = EVALUATED_AT
            )

            val rendered = FindingDocumentJson.render(document)

            FindingDocumentJson.parse(rendered, "document") shouldBe document
        }

        test("reads a document without findings") {
            val text = """{"source":"external","status":"SKIPPED","evaluatedAt":"2026-11-06T12:00:00Z"}"""

            FindingDocumentJson.parse(text, "document").findings shouldBe emptyList()
        }

        test("escapes hostile text in a finding of another tool") {
            val hidden = "\u202E"
            val text = """
                {"source":"external","status":"COMPLETE","evaluatedAt":"2026-11-06T12:00:00Z",
                 "findings":[{"ruleId":"R","source":"external","severity":"LOW","title":"a${hidden}b"}]}
            """.trimIndent()

            val finding = FindingDocumentJson.parse(text, "document").findings.single()

            finding.title shouldBe "a\\u{202E}b"
        }

        mapOf(
            "not JSON at all" to "nonsense{",
            "an array" to "[]",
            "an unknown source" to """{"source":"x","status":"COMPLETE","evaluatedAt":"2026-11-06T12:00:00Z"}""",
            "an unknown status" to """{"source":"external","status":"X","evaluatedAt":"2026-11-06T12:00:00Z"}""",
            "a bad time" to """{"source":"external","status":"COMPLETE","evaluatedAt":"yesterday"}"""
        ).forEach { (name, text) ->
            test("refuses $name and names the document") {
                val failure = shouldThrow<AssuranceInputException> { FindingDocumentJson.parse(text, "report.json") }

                failure.message.orEmpty() shouldContain "report.json"
            }
        }
    }
})
