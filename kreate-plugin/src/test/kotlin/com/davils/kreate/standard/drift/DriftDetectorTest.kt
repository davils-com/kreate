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
package com.davils.kreate.standard.drift

import com.davils.kore.testing.unsafeCodePointsIn
import com.davils.kreate.assurance.Severity
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.StandardSnapshot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val RFC: StandardDeclaration = StandardDeclaration(StandardIds.rfc(7386))

private val REVIEWED: StandardSnapshot = StandardSnapshot(
    title = "JSON Merge Patch",
    status = "PROPOSED STANDARD",
    revision = "1",
    errata = mapOf("4132" to ErratumState.VERIFIED)
)

private fun rulesOf(current: StandardSnapshot): List<String> {
    val findings = DriftDetector(RFC, REVIEWED, current).findings()
    return findings.map { finding -> finding.ruleId }
}

class DriftDetectorTest : FunSpec({
    test("reports nothing while nothing changed") {
        val findings = DriftDetector(RFC, REVIEWED, REVIEWED).findings()

        findings.shouldBeEmpty()
    }

    test("reports an obsoleted standard with its replacement") {
        val findings = DriftDetector(RFC, REVIEWED, REVIEWED.copy(replacedBy = listOf("RFC7396"))).findings()

        val finding = findings.single()

        finding.ruleId shouldBe "STANDARD_OBSOLETED"
        finding.severity shouldBe Severity.HIGH
        finding.title shouldBe "rfc:7386 is obsoleted by RFC7396"
        finding.module shouldBe "rfc:7386"
        finding.url shouldBe "https://www.rfc-editor.org/info/rfc7386"
    }

    test("reports a withdrawn standard") {
        rulesOf(REVIEWED.copy(withdrawn = true)) shouldBe listOf("STANDARD_OBSOLETED")
    }

    test("reports updates, revisions and changes of status") {
        rulesOf(REVIEWED.copy(updatedBy = listOf("RFC9999"))) shouldBe listOf("STANDARD_UPDATED")
        rulesOf(REVIEWED.copy(revision = "2")) shouldBe listOf("STANDARD_REVISED")
        rulesOf(REVIEWED.copy(status = "INTERNET STANDARD")) shouldBe listOf("STANDARD_STATUS_CHANGED")
    }

    test("reports a vendored suite whose files changed since the review") {
        val suite = StandardDeclaration(StandardIds.suite("xmlconf"))
        val reviewed = StandardSnapshot(title = "xmlts.tar.gz", contentDigest = "sha256:aa")

        val findings = DriftDetector(suite, reviewed, reviewed.copy(contentDigest = "sha256:bb")).findings()

        val finding = findings.single()

        finding.ruleId shouldBe "STANDARD_SUITE_MODIFIED"
        finding.severity shouldBe Severity.HIGH
        finding.title shouldBe "The vendored copy of suite:xmlconf changed since its review"
        finding.attributes shouldBe mapOf("reviewed" to "sha256:aa", "current" to "sha256:bb")
        val unchanged = DriftDetector(suite, reviewed, reviewed).findings()
        unchanged.shouldBeEmpty()
    }

    test("links new errata of a W3C specification to where they are published") {
        val xml = StandardDeclaration(StandardIds.w3c("xml"), errata = "https://github.com/w3c/xml/issues")
        val reviewed = StandardSnapshot(title = "XML", errata = mapOf("#1" to ErratumState.REPORTED))
        val current = reviewed.copy(errata = mapOf("#1" to ErratumState.VERIFIED, "#2" to ErratumState.REPORTED))

        val findings = DriftDetector(xml, reviewed, current).findings()

        findings.map { finding -> finding.ruleId } shouldBe listOf("STANDARD_ERRATA", "STANDARD_ERRATA_REPORTED")
        findings.map { finding -> finding.url }.distinct() shouldBe listOf("https://github.com/w3c/xml/issues")
        findings.first().title shouldBe "w3c:xml has 1 new confirmed errata: #1"
    }

    test("tells confirmed errata from reported ones and only reports new ones") {
        val errata = REVIEWED.errata + mapOf("5000" to ErratumState.HELD, "5001" to ErratumState.REPORTED)

        rulesOf(REVIEWED.copy(errata = errata)) shouldBe listOf("STANDARD_ERRATA", "STANDARD_ERRATA_REPORTED")
    }

    test("reports a reported erratum once it is verified") {
        val reviewed = REVIEWED.copy(errata = mapOf("5001" to ErratumState.REPORTED))
        val current = REVIEWED.copy(errata = mapOf("5001" to ErratumState.VERIFIED))

        val findings = DriftDetector(RFC, reviewed, current).findings()

        findings.map { finding -> finding.ruleId } shouldBe listOf("STANDARD_ERRATA")
        findings.single().url shouldBe "https://www.rfc-editor.org/errata/rfc7386"
    }

    test("reports a changed fingerprint of a watched page") {
        val document = StandardDeclaration(StandardIds.document("ECMA-404"), url = "https://example.org/404")
        val reviewed = StandardSnapshot("ECMA-404", fingerprint = "sha256:a")

        val findings = DriftDetector(document, reviewed, reviewed.copy(fingerprint = "sha256:b")).findings()

        findings.single().ruleId shouldBe "STANDARD_CHANGED"
        findings.single().url shouldBe "https://example.org/404"
    }

    test("escapes what the body sent") {
        val hostile = REVIEWED.copy(status = "STANDARD‮\n", replacedBy = listOf("RFC1\u0000"))

        val findings = DriftDetector(RFC, REVIEWED, hostile).findings()

        findings.flatMap { finding -> unsafeCodePointsIn(finding.title) }.shouldBeEmpty()
    }
})
