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
package com.davils.kreate.trivy.report

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.SourceLocation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun parse(report: String) = TrivyReportParser.parse(report) { path -> path }

class TrivyReportParserTest : FunSpec({
    context("a Trivy report") {
        test("reads a vulnerability with its fix") {
            val report = """
                {"Results":[{"Target":"gradle.lockfile","Vulnerabilities":[{
                  "VulnerabilityID":"CVE-2026-1","PkgName":"org.example:lib","InstalledVersion":"1.0.0",
                  "FixedVersion":"1.0.1","Severity":"HIGH","Title":"A flaw","PrimaryURL":"https://avd.example"
                }]}]}
            """.trimIndent()

            val finding = parse(report).single()

            finding.ruleId shouldBe "CVE-2026-1"
            finding.source shouldBe FindingSource.TRIVY
            finding.severity shouldBe Severity.HIGH
            finding.module shouldBe "org.example:lib"
            finding.version shouldBe "1.0.0"
            finding.fixedVersion shouldBe "1.0.1"
            finding.location shouldBe SourceLocation("gradle.lockfile")
        }

        test("names a license finding by its license") {
            val report = """
                {"Results":[{"Target":"lock","Licenses":[{"Name":"GPL-3.0","PkgName":"a:b","Severity":"HIGH"}]}]}
            """.trimIndent()

            parse(report).single().ruleId shouldBe "LICENSE:GPL-3.0"
        }

        test("keeps the line of a secret and only the masked match") {
            val report = """
                {"Results":[{"Target":"a.env","Secrets":[{
                  "RuleID":"github-pat","Severity":"CRITICAL","Title":"Token","StartLine":4,"Match":"token=*****"
                }]}]}
            """.trimIndent()

            val finding = parse(report).single()

            finding.ruleId shouldBe "github-pat"
            finding.location shouldBe SourceLocation("a.env", 4)
            finding.snippet shouldBe "token=*****"
        }

        test("reads a misconfiguration with its resolution") {
            val report = """
                {"Results":[{"Target":"Dockerfile","Misconfigurations":[{
                  "ID":"DS002","Severity":"HIGH","Title":"Root user","Resolution":"Add USER"
                }]}]}
            """.trimIndent()

            val finding = parse(report).single()

            finding.ruleId shouldBe "DS002"
            finding.description shouldBe "Add USER"
        }

        test("hands the target through the path mapping") {
            val report = """{"Results":[{"Target":"/tmp/x/a.env","Secrets":[{"RuleID":"r","Severity":"LOW"}]}]}"""

            val finding = TrivyReportParser.parse(report) { path -> path.removePrefix("/tmp/x/") }.single()

            finding.location?.path shouldBe "a.env"
        }

        test("reads a finding without a rating as unknown, for the gate to treat as serious") {
            val report = """{"Results":[{"Target":"t","Vulnerabilities":[{"VulnerabilityID":"CVE-1"}]}]}"""

            val finding = parse(report).single()

            finding.severity shouldBe Severity.UNKNOWN
            finding.gatingSeverity shouldBe Severity.HIGH
        }

        test("escapes hostile text from a database") {
            val hidden = "‮"
            val vulnerability = """{"VulnerabilityID":"C","Title":"a${hidden}b"}"""
            val report = """{"Results":[{"Target":"t","Vulnerabilities":[$vulnerability]}]}"""

            parse(report).single().title shouldBe "a\\u{202E}b"
        }

        test("finds nothing in a clean report") {
            parse("""{"SchemaVersion":2}""").shouldBeEmpty()
            parse("""{"Results":null}""").shouldBeEmpty()
        }

        test("refuses what is not a report") {
            shouldThrow<AssuranceInputException> { parse("{") }
            shouldThrow<AssuranceInputException> { parse("[]") }
        }
    }
})
