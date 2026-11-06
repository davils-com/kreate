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
package com.davils.kreate.assurance.baseline

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.finding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val ALL_COMPLETE: Set<FindingSource> = FindingSource.entries.toSet()

private fun entryFor(found: Finding, count: Int = 1): BaselineEntry = BaselineEntry(
    source = found.source.id,
    rule = found.ruleId,
    count = count,
    severity = found.severity,
    title = found.title,
    module = found.module,
    path = found.location?.path
)

class BaselineMatcherTest : FunSpec({
    context("a baseline") {
        test("covers a recorded finding") {
            val found = finding()

            val match = BaselineMatcher(listOf(entryFor(found))).partition(listOf(found), ALL_COMPLETE)

            match.baselined.size shouldBe 1
            match.kept.shouldBeEmpty()
        }

        test("keeps covering a finding that moved to another line") {
            val recorded = entryFor(finding(path = "a.kt", line = 3))
            val moved = finding(path = "a.kt", line = 90)

            val match = BaselineMatcher(listOf(recorded)).partition(listOf(moved), ALL_COMPLETE)

            match.kept.shouldBeEmpty()
        }

        test("reports a further occurrence as new, the one on the later line") {
            val first = finding(path = "a.kt", line = 3)
            val second = finding(path = "a.kt", line = 9)

            val match = BaselineMatcher(listOf(entryFor(first))).partition(listOf(second, first), ALL_COMPLETE)

            match.kept.single().location?.line shouldBe 9
        }

        test("treats two identical findings as separate occurrences") {
            val matcher = BaselineMatcher(listOf(entryFor(finding())))

            val match = matcher.partition(listOf(finding(), finding()), ALL_COMPLETE)

            match.baselined.size shouldBe 1
            match.kept.size shouldBe 1
        }

        test("does not cover a finding more severe than its limit") {
            val critical = finding(severity = Severity.CRITICAL)
            val matcher = BaselineMatcher(listOf(entryFor(critical)), Severity.HIGH)

            matcher.partition(listOf(critical), ALL_COMPLETE).kept.size shouldBe 1
        }

        test("counts an entry nothing matches any more as stale") {
            val matcher = BaselineMatcher(listOf(entryFor(finding(), count = 3)))

            val match = matcher.partition(listOf(finding()), ALL_COMPLETE)

            match.staleEntries shouldBe 2
        }

        test("does not call an entry stale when its source did not finish") {
            val match = BaselineMatcher(listOf(entryFor(finding()))).partition(emptyList(), emptySet())

            match.staleEntries shouldBe 0
        }
    }

    context("recording") {
        test("counts occurrences and leaves lines and versions out of the key") {
            val findings = listOf(
                finding(path = "a.kt", line = 1).copy(version = "1.0.0"),
                finding(path = "a.kt", line = 2).copy(version = "2.0.0")
            )

            Baseline.record(findings).single().count shouldBe 2
        }

        test("takes the most severe rating of a group") {
            val findings = listOf(finding(severity = Severity.LOW), finding(severity = Severity.HIGH))

            Baseline.record(findings).single().severity shouldBe Severity.HIGH
        }

        test("leaves out what the limit forbids to accept") {
            val findings = listOf(finding(severity = Severity.CRITICAL))

            Baseline.record(findings, Severity.HIGH).shouldBeEmpty()
        }
    }

    context("pruning") {
        test("lowers a count and never raises one") {
            val entry = entryFor(finding(), count = 3)

            val lowered = Baseline.prune(listOf(entry), listOf(finding()), ALL_COMPLETE)
            val unchanged = Baseline.prune(listOf(entryFor(finding())), listOf(finding(), finding()), ALL_COMPLETE)

            lowered.single().count shouldBe 1
            unchanged.single().count shouldBe 1
        }

        test("removes what no longer occurs") {
            Baseline.prune(listOf(entryFor(finding())), emptyList(), ALL_COMPLETE).shouldBeEmpty()
        }

        test("leaves an entry alone when its source did not finish") {
            Baseline.prune(listOf(entryFor(finding())), emptyList(), emptySet()).size shouldBe 1
        }
    }
})
