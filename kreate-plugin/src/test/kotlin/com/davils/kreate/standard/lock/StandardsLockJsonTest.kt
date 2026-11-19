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
package com.davils.kreate.standard.lock

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.StandardSnapshot
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

private val SNAPSHOT: StandardSnapshot = StandardSnapshot(
    title = "Universally Unique IDentifiers (UUIDs)",
    status = "PROPOSED STANDARD",
    published = "May 2024",
    updatedBy = listOf("RFC9999", "RFC9998"),
    errata = mapOf("8024" to ErratumState.REPORTED, "7929" to ErratumState.VERIFIED)
)

private val ENTRY: LockEntry = LockEntry(StandardIds.rfc(9562), LocalDate.parse("2026-11-19"), SNAPSHOT, "UUID v7")

private fun lockWith(id: String, reviewedOn: String): String =
    """{"schemaVersion":1,"standards":[{"id":"$id","reviewedOn":"$reviewedOn","snapshot":{"title":"t"}}]}"""

private fun failureOf(text: String): String {
    val failure = shouldThrow<AssuranceInputException> { StandardsLockJson.parse(text, "standards.lock") }
    return failure.message.orEmpty()
}

class StandardsLockJsonTest : FunSpec({
    test("reads back what it writes") {
        val lock = StandardsLock(listOf(ENTRY, ENTRY.copy(id = StandardIds.w3c("webauthn-3"), note = null)))

        val read = StandardsLockJson.parse(StandardsLockJson.render(lock), "standards.lock")

        read.entries shouldBe lock.entries.sortedBy { entry -> entry.id }.map { entry ->
            entry.copy(snapshot = entry.snapshot.copy(updatedBy = entry.snapshot.updatedBy.sorted()))
        }
    }

    test("writes sorted, stable JSON") {
        val text = StandardsLockJson.render(StandardsLock(listOf(ENTRY)))

        text shouldContain "\"schemaVersion\": 1"
        text shouldContain "\"id\": \"rfc:9562\""
        text.indexOf("\"7929\"") shouldBe text.indexOf("\"7929\"").coerceAtMost(text.indexOf("\"8024\""))
    }

    test("refuses unknown fields, schema versions and ids") {
        failureOf("""{"schemaVersion":1,"standards":[],"extra":1}""") shouldContain "unknown field"
        failureOf("""{"schemaVersion":2,"standards":[]}""") shouldContain "schemaVersion 1"
        failureOf(lockWith("rfc:x", "2026-01-01")) shouldContain "unknown id"
        failureOf(lockWith("rfc:1", "soon")) shouldContain "YYYY-MM-DD"
        failureOf("not json") shouldContain "not valid JSON"
    }

    test("escapes a hostile id") {
        failureOf(lockWith("rfc:\u202E1", "2026-01-01")) shouldContain "\\u{202E}"
    }

    test("merges reviews and forgets standards") {
        val other = ENTRY.copy(id = StandardIds.rfc(3339))
        val reviewed = ENTRY.copy(reviewedOn = LocalDate.parse("2027-01-01"))

        val lock = StandardsLock(listOf(ENTRY, other)).withReviews(listOf(reviewed))

        lock.entryOf(StandardIds.rfc(9562)) shouldBe reviewed
        lock.without(setOf(StandardIds.rfc(3339))).ids shouldBe setOf(StandardIds.rfc(9562))
    }

    test("keeps live snapshots by id") {
        val snapshots = mapOf(StandardIds.rfc(9562) to SNAPSHOT.copy(updatedBy = emptyList()))

        SnapshotsJson.parse(SnapshotsJson.render(snapshots), "snapshots.json") shouldBe snapshots
    }
})
