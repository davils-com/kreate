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
package com.davils.kreate.advisory.severity

import com.davils.kreate.assurance.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Cvss3Test : FunSpec({
    context("the base score") {
        mapOf(
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H" to 9.8,
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H" to 10.0,
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:N/A:N" to 4.3,
            "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N" to 5.5,
            "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N" to 7.5,
            "CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:C/C:L/I:L/A:N" to 5.4,
            "CVSS:3.1/AV:L/AC:H/PR:H/UI:N/S:C/C:H/I:L/A:L" to 6.7,
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N" to 0.0
        ).forEach { (vector, score) ->
            test("is $score for $vector") {
                Cvss3.scoreOf(vector) shouldBe score
            }
        }
    }

    context("the severity") {
        test("follows the CVSS bands") {
            Cvss3.severityOf("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H") shouldBe Severity.CRITICAL
            Cvss3.severityOf("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N") shouldBe Severity.HIGH
            Cvss3.severityOf("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N") shouldBe Severity.MEDIUM
            Cvss3.severityOf("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N") shouldBe Severity.LOW
        }

        test("is unknown for what it cannot read") {
            Cvss3.severityOf("CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:N/SI:N/SA:N") shouldBe null
            Cvss3.severityOf("CVSS:3.1/AV:X/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H") shouldBe null
            Cvss3.severityOf("nonsense") shouldBe null
        }
    }
})
