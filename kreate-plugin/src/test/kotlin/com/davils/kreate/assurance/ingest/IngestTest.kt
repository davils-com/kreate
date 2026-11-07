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
package com.davils.kreate.assurance.ingest

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val SARIF: String = """
    {"runs":[{"results":[
      {"ruleId":"detekt.style.MagicNumber","level":"warning","message":{"text":"Magic number 42"},
       "locations":[{"physicalLocation":{"artifactLocation":{"uri":"file:///work/app/src/A.kt"},"region":{"startLine":7}}}]},
      {"ruleId":"detekt.bugs.Unsafe","level":"error","message":{"text":"Unsafe cast"}}
    ]}]}
""".trimIndent()

private val COVERAGE: String = """
    <report name="app"><package name="a"><counter type="LINE" missed="1" covered="1"/></package>
    <counter type="INSTRUCTION" missed="10" covered="90"/><counter type="LINE" missed="25" covered="75"/></report>
""".trimIndent()

class IngestTest : FunSpec({
    context("a Detekt SARIF report") {
        test("becomes findings that only inform, with relative locations") {
            val findings = SarifIngest.findingsOf(SARIF, "file:/work/app/")

            findings.map { finding -> finding.ruleId } shouldBe listOf("detekt.style.MagicNumber", "detekt.bugs.Unsafe")
            findings.first().location?.line shouldBe 7
            findings.map { finding -> finding.severity } shouldBe listOf(Severity.LOW, Severity.MEDIUM)
            findings.all { finding -> REPORT_ONLY_ATTRIBUTE in finding.attributes } shouldBe true
        }

        test("that is no SARIF gives nothing") {
            SarifIngest.findingsOf("nonsense", "").shouldBeEmpty()
        }
    }

    context("a coverage report") {
        test("gives the line coverage of the whole report") {
            CoverageIngest.lineCoverageOf(COVERAGE.toByteArray()) shouldBe (75.0 plusOrMinus 0.01)
        }

        test("below the minimum becomes a finding, at or above it none") {
            CoverageIngest.findingsOf(75.0, 80).single().ruleId shouldBe "COVERAGE_BELOW_MINIMUM"
            CoverageIngest.findingsOf(80.0, 80).shouldBeEmpty()
            CoverageIngest.findingsOf(75.0, null).shouldBeEmpty()
        }

        test("does not resolve external entities") {
            val hostile = "<!DOCTYPE r [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><report>&e;</report>"

            CoverageIngest.lineCoverageOf(hostile.toByteArray()).shouldBeNull()
        }
    }

    context("an API dump") {
        test("that differs from the code is reported, line endings aside") {
            ApiDumpIngest.findingsOf("a\r\nb\n", "a\nb", "api/app.api").shouldBeEmpty()
            ApiDumpIngest.findingsOf("a\n", "b\n", "api/app.api").single().location?.path shouldBe "api/app.api"
            ApiDumpIngest.findingsOf("a\n", null, "api/app.api").shouldBeEmpty()
        }
    }
})
