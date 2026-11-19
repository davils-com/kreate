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

import com.davils.kore.core.outcome.Outcome
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.standard.lock.LockEntry
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.StandardSnapshot
import com.davils.kreate.standard.provider.StandardProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val NOW: Instant = Instant.parse("2026-11-19T12:00:00Z")

private val ANSWERED: Instant = Instant.parse("2026-11-18T12:00:00Z")

private val RFC: StandardDeclaration = StandardDeclaration(StandardIds.rfc(7386))

private val DIN: StandardDeclaration = StandardDeclaration(StandardIds.din("DIN 5008"), reviewEverySeconds = 1)

private fun auditorOf(provider: StandardProvider): StandardsAuditor =
    StandardsAuditor({ listOf(provider) }, { ANSWERED }, Clock.fixed(NOW, ZoneOffset.UTC))

private fun lockOf(snapshot: StandardSnapshot): StandardsLock =
    StandardsLock(listOf(LockEntry(RFC.id, LocalDate.parse("2026-01-01"), snapshot)))

class StandardsAuditorTest : FunSpec({
    val reviewed = StandardSnapshot("JSON Merge Patch")

    test("skips a project without standards") {
        val result = auditorOf(FixedProvider(emptyMap())).audit(emptyList(), StandardsLock(), offline = false)

        result.document.status shouldBe DocumentStatus.SKIPPED
    }

    test("is complete without asking anyone when no standard can be checked") {
        val provider = FixedProvider(emptyMap())

        val result = auditorOf(provider).audit(listOf(DIN), StandardsLock(), offline = true)

        result.document.status shouldBe DocumentStatus.COMPLETE
        result.snapshots.keys shouldBe setOf(DIN.id)
        provider.asked shouldBe 0
    }

    test("asks nobody in offline mode") {
        val provider = FixedProvider(emptyMap())

        val result = auditorOf(provider).audit(listOf(RFC), StandardsLock(), offline = true)

        result.document.status shouldBe DocumentStatus.INCOMPLETE
        result.document.reason.orEmpty() shouldContain "offline"
        provider.asked shouldBe 0
    }

    test("reports drift against the review and dates its data by the oldest answer") {
        val current = reviewed.copy(replacedBy = listOf("RFC7396"))
        val provider = FixedProvider(mapOf("rfc:7386" to Outcome.Success(current)))

        val result = auditorOf(provider).audit(listOf(RFC), lockOf(reviewed), offline = false)

        result.document.status shouldBe DocumentStatus.COMPLETE
        result.document.findings.single().ruleId shouldBe "STANDARD_OBSOLETED"
        result.document.dataAsOf shouldBe ANSWERED
        result.snapshots[RFC.id] shouldBe current
    }

    test("leaves an unreviewed standard to the inventory") {
        val provider = FixedProvider(mapOf("rfc:7386" to Outcome.Success(reviewed)))

        val result = auditorOf(provider).audit(listOf(RFC), StandardsLock(), offline = false)

        result.document.findings.shouldBeEmpty()
    }

    test("reports a standard the body does not know") {
        val provider = FixedProvider(mapOf("rfc:7386" to Outcome.Failure(LookupFailure.Unknown("gone"))))

        val result = auditorOf(provider).audit(listOf(RFC), lockOf(reviewed), offline = false)

        result.document.status shouldBe DocumentStatus.COMPLETE
        result.document.findings.single().ruleId shouldBe "STANDARD_UNKNOWN"
    }

    test("is incomplete and says why when a body cannot be reached") {
        val provider = FixedProvider(mapOf("rfc:7386" to Outcome.Failure(LookupFailure.Unavailable("HTTP 503"))))

        val result = auditorOf(provider).audit(listOf(RFC), lockOf(reviewed), offline = false)

        result.document.status shouldBe DocumentStatus.INCOMPLETE
        result.document.reason shouldBe "HTTP 503"
        result.snapshots.containsKey(RFC.id) shouldBe false
    }
})
