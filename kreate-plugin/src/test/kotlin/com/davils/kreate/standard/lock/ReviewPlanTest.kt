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

import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.model.StandardSnapshot
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.parse("2026-11-19")

private val RFC: StandardDeclaration = StandardDeclaration(StandardIds.rfc(9562))

private val W3C: StandardDeclaration = StandardDeclaration(StandardIds.w3c("webauthn-3"))

private val SNAPSHOT: StandardSnapshot = StandardSnapshot("UUIDs")

class ReviewPlanTest : FunSpec({
    test("lists the standards that wait for a review") {
        val lock = StandardsLock(listOf(LockEntry(RFC.id, TODAY, SNAPSHOT)))
        val plan = ReviewPlan(listOf(RFC, W3C), lock, mapOf(RFC.id to SNAPSHOT, W3C.id to SNAPSHOT))

        plan.pending shouldBe listOf(W3C.id)
    }

    test("records the review with the snapshot of the audit") {
        val plan = ReviewPlan(listOf(RFC), StandardsLock(), mapOf(RFC.id to SNAPSHOT))

        val lock = plan.reviewed(plan.chosen(listOf("rfc:9562"), all = false), "Read it.", TODAY)

        lock.entries shouldBe listOf(LockEntry(RFC.id, TODAY, SNAPSHOT, "Read it."))
    }

    test("suggests the declared standard closest to an unknown one") {
        val plan = ReviewPlan(listOf(RFC, W3C), StandardsLock(), emptyMap())

        val failure = shouldThrow<ReviewException> { plan.chosen(listOf("rfc:9526"), all = false) }

        failure.message.orEmpty() shouldContain "Did you mean rfc:9562?"
    }

    test("refuses a standard the audit could not read") {
        val plan = ReviewPlan(listOf(RFC), StandardsLock(), emptyMap())

        val failure = shouldThrow<ReviewException> { plan.reviewed(listOf(RFC.id), "Read it.", TODAY) }

        failure.message.orEmpty() shouldContain "Cannot review rfc:9562"
    }

    test("finds the reviews of standards that are no longer declared") {
        val lock = StandardsLock(listOf(LockEntry(RFC.id, TODAY, SNAPSHOT), LockEntry(W3C.id, TODAY, SNAPSHOT)))

        ReviewPlan(listOf(RFC), lock, emptyMap()).orphaned shouldBe setOf(W3C.id)
    }
})
