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
package com.davils.kreate.assurance.policy

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.baseline.BaselineEntry
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.finding.finding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.parse("2026-11-06")

private val ALL_COMPLETE: Set<FindingSource> = FindingSource.entries.toSet()

private fun filter(
    minSeverity: Severity = Severity.UNKNOWN,
    rules: List<IgnoreRule> = emptyList(),
    entries: List<BaselineEntry> = emptyList(),
    unignorable: Set<Severity> = emptySet()
): FindingFilter = FindingFilter(minSeverity, rules, TODAY, BaselineMatcher(entries), unignorable)

class FindingFilterTest : FunSpec({
    context("the minimum severity") {
        test("drops what is below it, before anything else looks at it") {
            val low = finding(severity = Severity.LOW)

            val outcome = filter(minSeverity = Severity.HIGH).apply(listOf(low), ALL_COMPLETE)

            outcome.kept.shouldBeEmpty()
            outcome.suppressed.shouldBeEmpty()
        }

        test("cannot hide an advisory without a rating") {
            val advisory = finding(source = FindingSource.OSV, severity = Severity.UNKNOWN)

            filter(minSeverity = Severity.CRITICAL).apply(listOf(advisory), ALL_COMPLETE).kept shouldContainExactly
                listOf(advisory)
        }
    }

    context("ignore rules") {
        test("suppress a finding by its rule") {
            val outcome = filter(rules = listOf(IgnoreRule("RULE", "Accepted"))).apply(listOf(finding()), ALL_COMPLETE)

            outcome.kept.shouldBeEmpty()
            outcome.suppressed shouldContainExactly listOf(finding())
        }

        test("match an alias of the finding") {
            val aliased = finding().copy(aliases = listOf("CVE-1"))

            val outcome = filter(rules = listOf(IgnoreRule("CVE-1", "Accepted"))).apply(listOf(aliased), ALL_COMPLETE)

            outcome.suppressed shouldContainExactly listOf(aliased)
        }

        test("stay on the module they name") {
            val rule = IgnoreRule("RULE", "Accepted", module = "com.example:other")

            filter(rules = listOf(rule)).apply(listOf(finding()), ALL_COMPLETE).kept shouldContainExactly
                listOf(finding())
        }

        test("still hold on the day they expire") {
            val rule = IgnoreRule("RULE", "Accepted", expires = TODAY)

            val outcome = filter(rules = listOf(rule)).apply(listOf(finding()), ALL_COMPLETE)

            outcome.kept.shouldBeEmpty()
        }

        test("stop matching after they expired, and are reported") {
            val rule = IgnoreRule("RULE", "Accepted", expires = TODAY.minusDays(1))

            val outcome = filter(rules = listOf(rule)).apply(listOf(finding()), ALL_COMPLETE)

            outcome.kept shouldContainExactly listOf(finding())
            outcome.expiredRules shouldContainExactly listOf(rule)
        }
    }

    context("an unignorable severity") {
        val critical = finding(severity = Severity.CRITICAL)
        val protectedFilter = filter(
            rules = listOf(IgnoreRule("RULE", "Accepted")),
            entries = listOf(BaselineEntry("inspection", "RULE", 1, Severity.CRITICAL, "t", "com.example:lib")),
            unignorable = setOf(Severity.CRITICAL)
        )

        test("stays reported past an ignore rule and the baseline") {
            val outcome = protectedFilter.apply(listOf(critical), ALL_COMPLETE)

            outcome.kept.single().ruleId shouldBe "RULE"
            outcome.baselined.shouldBeEmpty()
            outcome.suppressed.shouldBeEmpty()
        }

        test("says that an ignore rule tried to hide it") {
            val outcome = protectedFilter.apply(listOf(critical), ALL_COMPLETE)

            outcome.kept.single().attributes["ignoreForbidden"] shouldBe "true"
        }
    }

    context("the baseline") {
        test("runs after the ignore rules, so an ignored finding spends no budget") {
            val entry = BaselineEntry("inspection", "RULE", 1, Severity.HIGH, "t", "com.example:lib")
            val outcome = filter(rules = listOf(IgnoreRule("RULE", "Accepted")), entries = listOf(entry))
                .apply(listOf(finding()), ALL_COMPLETE)

            outcome.suppressed.size shouldBe 1
            outcome.baselined.shouldBeEmpty()
        }
    }
})
