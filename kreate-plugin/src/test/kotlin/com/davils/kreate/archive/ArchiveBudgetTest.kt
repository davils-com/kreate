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

package com.davils.kreate.archive

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArchiveBudgetTest : FunSpec({
    test("adds up what it is charged") {
        val budget = ArchiveBudget()

        budget.chargeBytes(10)
        budget.chargeBytes(5)
        budget.chargeEntry()

        budget.extractedBytes shouldBe 15L
        budget.entries shouldBe 1
    }

    test("allows exactly the limit") {
        val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = 10))

        budget.chargeBytes(10)

        shouldThrow<ArchiveException.LimitExceeded> { budget.chargeBytes(1) }
    }

    test("does not overflow on a hostile size") {
        val budget = ArchiveBudget(ArchiveLimits(maxExtractedBytes = Long.MAX_VALUE))
        budget.chargeBytes(Long.MAX_VALUE)

        shouldThrow<ArchiveException.LimitExceeded> { budget.chargeBytes(Long.MAX_VALUE) }
    }

    test("returns the result of a nested read") {
        ArchiveBudget().nested { 42 } shouldBe 42
    }

    test("leaves the nesting level after a failure") {
        val budget = ArchiveBudget(ArchiveLimits(maxNestingDepth = 1))

        shouldThrow<IllegalStateException> { budget.nested { error("failed") } }

        budget.nested { 1 } shouldBe 1
    }

    test("stops at the nesting limit") {
        val budget = ArchiveBudget(ArchiveLimits(maxNestingDepth = 1))

        shouldThrow<ArchiveException.LimitExceeded> { budget.nested { budget.nested { } } }
    }
})
