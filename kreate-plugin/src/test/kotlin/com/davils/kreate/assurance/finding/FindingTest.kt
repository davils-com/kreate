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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FindingTest : FunSpec({
    context("the gating severity") {
        test("is the severity of a rated finding") {
            finding(severity = Severity.LOW).gatingSeverity shouldBe Severity.LOW
        }

        test("treats an advisory without a rating as serious") {
            finding(source = FindingSource.OSV, severity = Severity.UNKNOWN).gatingSeverity shouldBe Severity.HIGH
            finding(source = FindingSource.TRIVY, severity = Severity.UNKNOWN).gatingSeverity shouldBe Severity.HIGH
        }

        test("leaves another unrated finding unknown") {
            finding(source = FindingSource.QUALITY, severity = Severity.UNKNOWN).gatingSeverity shouldBe
                Severity.UNKNOWN
        }
    }

    context("the fingerprint") {
        test("is stable for the same finding") {
            finding(path = "a.kt", line = 3).fingerprint shouldBe finding(path = "a.kt", line = 3).fingerprint
        }

        test("changes with the line") {
            finding(path = "a.kt", line = 3).fingerprint shouldNotBe finding(path = "a.kt", line = 4).fingerprint
        }

        test("ignores the title, which a source may reword") {
            val reworded = finding().copy(title = "Reworded")

            reworded.fingerprint shouldBe finding().fingerprint
        }
    }

    context("severity names") {
        test("understands the names advisories use") {
            parseSeverity("moderate") shouldBe Severity.MEDIUM
            parseSeverity("NONE") shouldBe Severity.LOW
            parseSeverity("critical") shouldBe Severity.CRITICAL
        }

        test("reads anything else as unknown") {
            parseSeverity("catastrophic") shouldBe Severity.UNKNOWN
        }

        test("orders the unknown severity below low") {
            Severity.LOW.isAtLeast(Severity.UNKNOWN) shouldBe true
            Severity.UNKNOWN.isAtLeast(Severity.LOW) shouldBe false
        }
    }
})
