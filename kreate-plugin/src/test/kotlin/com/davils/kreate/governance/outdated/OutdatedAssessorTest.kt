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
package com.davils.kreate.governance.outdated

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.provenance.depsdev.PackageHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.time.Instant

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private val HISTORY: PackageHistory = PackageHistory(
    mapOf(
        "1.0.0" to Instant.parse("2020-01-01T00:00:00Z"),
        "2.0.0" to Instant.parse("2021-01-01T00:00:00Z"),
        "3.0.0" to Instant.parse("2022-01-01T00:00:00Z"),
        "4.0.0-rc1" to Instant.parse("2023-01-01T00:00:00Z")
    ),
    latest = "3.0.0"
)

private fun assess(settings: OutdatedSettings, history: PackageHistory = HISTORY): OutdatedResult =
    OutdatedAssessor(settings).assess(listOf(MODULE to history))

class OutdatedAssessorTest : FunSpec({
    context("a module") {
        test("counts the major releases it lags behind, without pre-releases") {
            val module = assess(OutdatedSettings(null, null)).modules.single()

            module.latest shouldBe "3.0.0"
            module.majorsBehind shouldBe 2
            module.libyears shouldBe (2.0 plusOrMinus 0.01)
        }

        test("on the latest release is not outdated") {
            val current = PackageHistory(HISTORY.published, latest = "1.0.0")

            assess(OutdatedSettings(0, 0.0), current).modules.shouldBeEmpty()
        }

        test("is reported beyond the allowed majors") {
            val result = assess(OutdatedSettings(1, null))

            val finding = result.findings.single()
            finding.ruleId shouldBe "OUTDATED_MAJOR"
            finding.fixedVersion shouldBe "3.0.0"
        }

        test("is not reported within the allowed majors") {
            assess(OutdatedSettings(2, null)).findings.shouldBeEmpty()
        }
    }

    context("libyears") {
        test("beyond the limit are reported once for the build") {
            val finding = assess(OutdatedSettings(null, 1.0)).findings.single()

            finding.ruleId shouldBe "LIBYEAR_EXCEEDED"
            finding.attributes["libyears"] shouldBe "2.0"
            finding.module shouldBe null
        }

        test("within the limit are not reported") {
            assess(OutdatedSettings(null, 5.0)).findings.shouldBeEmpty()
        }
    }
})
