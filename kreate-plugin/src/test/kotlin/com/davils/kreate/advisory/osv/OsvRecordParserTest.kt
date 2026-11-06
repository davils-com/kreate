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
package com.davils.kreate.advisory.osv

import com.davils.kreate.assurance.Severity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val MODULE: String = "org.example:lib"

class OsvRecordParserTest : FunSpec({
    context("an OSV record") {
        test("computes its severity from the CVSS vector") {
            val record = """
                {"id":"GHSA-1","modified":"2026-01-01T00:00:00Z","aliases":["CVE-1"],"summary":"A flaw",
                 "severity":[{"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}],
                 "database_specific":{"severity":"LOW"}}
            """.trimIndent()

            val parsed = OsvRecordParser.parse(record, "GHSA-1")

            parsed.severity shouldBe Severity.CRITICAL
            parsed.aliases shouldBe listOf("CVE-1")
        }

        test("takes the most severe of several vectors") {
            val record = """
                {"id":"G","severity":[
                  {"type":"CVSS_V3","score":"CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N"},
                  {"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}]}
            """.trimIndent()

            OsvRecordParser.parse(record, "G").severity shouldBe Severity.CRITICAL
        }

        test("falls back to the database rating for a CVSS 4 vector") {
            val record = """
                {"id":"G","severity":[{"type":"CVSS_V4","score":"CVSS:4.0/AV:N"}],
                 "database_specific":{"severity":"MODERATE"}}
            """.trimIndent()

            OsvRecordParser.parse(record, "G").severity shouldBe Severity.MEDIUM
        }

        test("is unknown without any rating") {
            OsvRecordParser.parse("""{"id":"G"}""", "G").severity shouldBe Severity.UNKNOWN
        }

        test("notices a withdrawn record") {
            OsvRecordParser.parse("""{"id":"G","withdrawn":"2026-01-01T00:00:00Z"}""", "G").isWithdrawn shouldBe true
        }

        test("reads the ranges per package and ignores git ranges") {
            val record = """
                {"id":"G","affected":[
                  {"package":{"name":"$MODULE"},"ranges":[
                    {"type":"GIT","events":[{"introduced":"abc"},{"fixed":"def"}]},
                    {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"1.0.1"},
                      {"introduced":"2.0.0"},{"fixed":"2.0.3"}]}]},
                  {"package":{"name":"org.example:other"},"ranges":[
                    {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"9.0.0"}]}]}]}
            """.trimIndent()

            val ranges = OsvRecordParser.parse(record, "G").ranges

            ranges[MODULE] shouldBe listOf(OsvRange("0", "1.0.1"), OsvRange("2.0.0", "2.0.3"))
            ranges["org.example:other"] shouldBe listOf(OsvRange("0", "9.0.0"))
        }

        test("refuses what is not a record") {
            shouldThrow<OsvUnavailableException> { OsvRecordParser.parse("{", "G") }
            shouldThrow<OsvUnavailableException> { OsvRecordParser.parse("[]", "G") }
        }
    }

    context("the fixed version") {
        val ranges = listOf(OsvRange("0", "1.0.1"), OsvRange("2.0.0", "2.0.3"))

        test("comes from the range holding the installed version") {
            fixedVersionFor("2.0.1", ranges) shouldBe "2.0.3"
            fixedVersionFor("0.9.0", ranges) shouldBe "1.0.1"
        }

        test("falls back to the smallest later fix for a version outside every range") {
            fixedVersionFor("1.5.0", ranges) shouldBe "2.0.3"
        }

        test("is absent when nothing later fixes it") {
            fixedVersionFor("3.0.0", ranges) shouldBe null
            fixedVersionFor("1.0.0", emptyList()) shouldBe null
        }

        test("may be a pre-release, as the source publishes it") {
            fixedVersionFor("2.4.0", listOf(OsvRange("0", "2.4.20-Beta1"))) shouldBe "2.4.20-Beta1"
        }
    }
})
