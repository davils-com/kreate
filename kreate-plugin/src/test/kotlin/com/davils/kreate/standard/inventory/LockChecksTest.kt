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

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.standard.lock.LockEntry
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.StandardSnapshot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDate

private val REVIEWED_ON: LocalDate = LocalDate.parse("2026-01-01")

private val RFC: StandardDeclaration = StandardDeclaration(StandardIds.rfc(9562))

private val DIN: StandardDeclaration =
    StandardDeclaration(StandardIds.din("DIN 5008"), reviewEverySeconds = Duration.ofDays(30).seconds)

private fun entryOf(declaration: StandardDeclaration): LockEntry =
    LockEntry(declaration.id, REVIEWED_ON, StandardSnapshot("A standard"))

private fun rulesOf(findings: List<Finding>): List<String> =
    findings.map { finding -> "${finding.ruleId} ${finding.module}" }

class LockChecksTest : FunSpec({
    test("reports a standard that was never reviewed") {
        val findings = LockChecks(listOf(RFC), null, "standards.lock").findingsOf(StandardsLock(), REVIEWED_ON)

        rulesOf(findings) shouldBe listOf("STANDARD_UNREVIEWED rfc:9562")
        findings.single().location?.path shouldBe "standards.lock"
        findings.single().aliases shouldBe listOf("rfc:9562")
    }

    test("reports a review once its interval has passed") {
        val lock = StandardsLock(listOf(entryOf(RFC), entryOf(DIN)))
        val checks = LockChecks(listOf(RFC, DIN), null, "standards.lock")

        checks.findingsOf(lock, REVIEWED_ON.plusDays(30)).shouldBeEmpty()
        rulesOf(checks.findingsOf(lock, REVIEWED_ON.plusDays(31))) shouldBe listOf("STANDARD_REVIEW_OVERDUE din:5008")
    }

    test("applies the default interval to standards without one of their own") {
        val lock = StandardsLock(listOf(entryOf(RFC), entryOf(DIN)))
        val checks = LockChecks(listOf(RFC, DIN), Duration.ofDays(365), "standards.lock")

        rulesOf(checks.findingsOf(lock, REVIEWED_ON.plusDays(366))) shouldBe
            listOf("STANDARD_REVIEW_OVERDUE rfc:9562", "STANDARD_REVIEW_OVERDUE din:5008")
    }

    test("reports a review of a standard that is no longer declared") {
        val lock = StandardsLock(listOf(entryOf(RFC), entryOf(DIN)))

        val findings = LockChecks(listOf(RFC), null, "standards.lock").findingsOf(lock, REVIEWED_ON)

        rulesOf(findings) shouldBe listOf("STANDARD_LOCK_ORPHANED din:5008")
    }
})
