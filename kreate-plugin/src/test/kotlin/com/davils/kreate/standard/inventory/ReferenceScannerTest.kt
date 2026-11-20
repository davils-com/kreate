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
package com.davils.kreate.standard.inventory

import com.davils.kore.testing.HostileFragments
import com.davils.kreate.standard.model.StandardIds
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.io.File

private fun idsIn(line: String): List<String> = ReferencePatterns.idsIn(line).map { id -> id.toString() }

class ReferenceScannerTest : FunSpec({
    context("a line") {
        test("names RFCs in text and in links") {
            val line = "Merges as RFC 7396 says, see https://www.rfc-editor.org/rfc/rfc6902 and RFC-9535."

            idsIn(line) shouldContainExactly listOf("rfc:7396", "rfc:9535", "rfc:6902")
            idsIn("https://datatracker.ietf.org/doc/html/rfc9110#section-13.1.2") shouldBe listOf("rfc:9110")
        }

        test("names drafts without their revision") {
            idsIn("draft-ietf-httpbis-resumable-upload-05 and JSON Schema draft-07") shouldBe
                listOf("draft:draft-ietf-httpbis-resumable-upload")
        }

        test("names ISO, W3C, WHATWG, Unicode and DIN standards") {
            idsIn("Dates follow ISO 8601-1:2019 and ISO/IEC 27001.") shouldBe listOf("iso:8601-1", "iso:iec-27001")
            idsIn("<a href=\"https://www.w3.org/TR/WCAG22/\">") shouldBe listOf("w3c:wcag22")
            idsIn("https://url.spec.whatwg.org/#concept-url") shouldBe listOf("whatwg:url")
            idsIn("Identifiers follow UAX #31 and UTS #39.") shouldBe listOf("unicode:uax-31", "unicode:uts-39")
            idsIn("Letters follow DIN 5008 and DIN EN ISO 9001.") shouldBe listOf("din:5008", "din:en-iso-9001")
        }

        test("names W3C specifications by their old and dated addresses") {
            idsIn("https://www.w3.org/TR/REC-xml/#sec-white-space") shouldBe listOf("w3c:xml")
            idsIn("http://www.w3.org/TR/REC-xml-names/") shouldBe listOf("w3c:xml-names")
            idsIn("https://www.w3.org/TR/xml-c14n") shouldBe listOf("w3c:xml-c14n10")
            idsIn("https://www.w3.org/TR/2006/REC-xml11-20060816/") shouldBe listOf("w3c:xml11")
            idsIn("https://www.w3.org/TR/xpath") shouldBe listOf("w3c:xpath")
        }

        test("passes over charsets and numbers that name no standard") {
            idsIn("val charset = \"ISO-8859-1\"; RFC 0; RFC 123456; draft-07").shouldBeEmpty()
        }

        test("survives hostile text") {
            checkAll(Arb.list(Arb.element(HostileFragments.all), 0..32)) { fragments ->
                idsIn(fragments.joinToString("RFC ")).all { id -> id.startsWith("rfc:") } shouldBe true
            }
        }
    }

    context("the files of a project") {
        test("give one finding per undeclared standard at its first place") {
            val directory = tempdir()
            val main = File(directory, "Main.kt").apply { writeText("// none\nval a = 1 // RFC 7396\n") }
            val readme = File(directory, "README.md").apply { writeText("RFC 7396 and RFC 9562\n") }
            val scanner = ReferenceScanner(setOf(StandardIds.rfc(9562)))

            val files = listOf(ScannedFile("src/Main.kt", main), ScannedFile("README.md", readme))

            val findings = scanner.findingsOf(files)

            val finding = findings.single()
            finding.ruleId shouldBe "STANDARD_UNDECLARED"
            finding.module shouldBe "rfc:7396"
            finding.location?.path shouldBe "README.md"
            finding.location?.line shouldBe 1
            finding.attributes shouldBe mapOf("occurrences" to "2", "files" to "2")
        }

        test("report a link without a version even when one of its versions is declared") {
            val directory = tempdir()
            val docs = File(directory, "xpath.md")
            docs.writeText("See https://www.w3.org/TR/xpath and w3.org/TR/xpath/\n")
            val scanner = ReferenceScanner(setOf(StandardIds.w3c("xpath-10")))

            val finding = scanner.findingsOf(listOf(ScannedFile("docs/xpath.md", docs))).single()

            finding.ruleId shouldBe "STANDARD_VERSIONLESS"
            finding.module shouldBe "w3c:xpath"
            finding.title shouldBe "docs/xpath.md links w3c:xpath, which names no version; " +
                "link and declare w3c:xpath-10 or w3c:xpath-31"
            finding.attributes shouldBe mapOf("occurrences" to "1", "versions" to "w3c:xpath-10 or w3c:xpath-31")
        }

        test("count an old address of a declared specification as declared") {
            val readme = File(tempdir(), "README.md").apply { writeText("https://www.w3.org/TR/REC-xml/\n") }

            val scanner = ReferenceScanner(setOf(StandardIds.w3c("xml")))

            val findings = scanner.findingsOf(listOf(ScannedFile("README.md", readme)))

            findings.shouldBeEmpty()
        }

        test("pass over binary files") {
            val binary = File(tempdir(), "image.png")
            binary.writeBytes(byteArrayOf(0) + "RFC 1".toByteArray())

            val findings = ReferenceScanner(emptySet()).findingsOf(listOf(ScannedFile("image.png", binary)))

            findings.shouldBeEmpty()
        }
    }
})
