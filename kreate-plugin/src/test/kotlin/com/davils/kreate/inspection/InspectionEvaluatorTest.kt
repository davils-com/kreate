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
package com.davils.kreate.inspection

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.inspection.scan.Hit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private fun findingsOf(
    hits: List<Hit>,
    failScore: Int = 30,
    failOnRisk: Boolean = false,
    trusted: Set<String> = emptySet(),
    module: ModuleCoordinate = MODULE
): List<Finding> {
    val evaluator = InspectionEvaluator(InspectionSettings(failScore, failOnRisk, trusted))
    return evaluator.evaluate(InspectedModule(module, hits))
}

private fun rulesOf(hits: List<Hit>, failScore: Int = 30, module: ModuleCoordinate = MODULE): List<String> =
    findingsOf(hits, failScore, module = module).map { finding -> finding.ruleId }

private fun List<Finding>.risk(): Finding = single { finding -> finding.ruleId == "RISK_SCORE" }

class InspectionEvaluatorTest : FunSpec({
    context("a finding") {
        test("names the module and explains itself") {
            val finding = findingsOf(listOf(Hit("RAW_SOCKET", "a.class"))).single()

            finding.module shouldBe "org.example:lib"
            finding.version shouldBe "1.0.0"
            finding.description.isNotEmpty() shouldBe true
            finding.location?.path shouldBe "a.class"
        }

        test("only informs, because many sound libraries open sockets") {
            val finding = findingsOf(listOf(Hit("RAW_SOCKET", "a.class"))).single()

            (REPORT_ONLY_ATTRIBUTE in finding.attributes) shouldBe true
        }

        test("stands for all its occurrences in the module") {
            val hits = listOf(Hit("RAW_SOCKET", "a.class"), Hit("RAW_SOCKET", "b.class"))

            findingsOf(hits).single().attributes["occurrences"] shouldBe "2"
        }
    }

    context("addresses") {
        test("on a well known host are expected") {
            rulesOf(listOf(Hit("HARDCODED_URL", "a.class", "www.apache.org"))).shouldBeEmpty()
        }

        test("on a host the build trusts are expected") {
            val hits = listOf(Hit("HARDCODED_URL", "a.class", "api.corp.example"))

            findingsOf(hits, trusted = setOf("corp.example")).shouldBeEmpty()
        }

        test("on a look-alike of a trusted host are not") {
            val hits = listOf(Hit("HARDCODED_URL", "a.class", "apache.org.evil.example"))

            rulesOf(hits) shouldBe listOf("HARDCODED_URL")
        }

        test("stay apart per host") {
            val one = Hit("HARDCODED_URL", "a.class", "one.example")
            val two = Hit("HARDCODED_URL", "a.class", "two.example")

            findingsOf(listOf(one, two)).size shouldBe 2
        }
    }

    context("a repository of a POM") {
        test("on a trusted host is expected") {
            rulesOf(listOf(Hit("POM_REPOSITORY", "pom.xml", "https://repo.maven.org/maven2"))).shouldBeEmpty()
        }

        test("on any other host is not") {
            val hits = listOf(Hit("POM_REPOSITORY", "pom.xml", "http://repo.evil.example"))

            rulesOf(hits) shouldBe listOf("POM_REPOSITORY")
        }

        test("without a readable address is not") {
            val hits = listOf(Hit("POM_REPOSITORY", "pom.xml", "not a url"))

            rulesOf(hits) shouldBe listOf("POM_REPOSITORY")
        }
    }

    context("a class in a platform package") {
        val hit = Hit("SHADOWED_PLATFORM_CLASS", "kotlin/Unit.class")

        test("is expected from Kotlin itself") {
            val stdlib = ModuleCoordinate("org.jetbrains.kotlin", "kotlin-stdlib", "2.4.0")

            rulesOf(listOf(hit), module = stdlib).shouldBeEmpty()
        }

        test("is not expected from anyone else") {
            rulesOf(listOf(hit)) shouldBe listOf("SHADOWED_PLATFORM_CLASS")
        }

        test("is not expected from a group that only looks like Kotlin's") {
            val lookalike = ModuleCoordinate("org.jetbrains.kotlinevil", "lib", "1.0.0")

            rulesOf(listOf(hit), module = lookalike) shouldBe listOf("SHADOWED_PLATFORM_CLASS")
        }
    }

    context("the risk score") {
        val risky = listOf(Hit("BACKDOOR_PATTERN", "a.class"))

        test("is reported at the threshold, without blocking by default") {
            val risk = findingsOf(risky).risk()

            risk.attributes["score"] shouldBe "40"
            (REPORT_ONLY_ATTRIBUTE in risk.attributes) shouldBe true
        }

        test("blocks when the build asks for it") {
            val risk = findingsOf(risky, failOnRisk = true).risk()

            (REPORT_ONLY_ATTRIBUTE in risk.attributes) shouldBe false
        }

        test("stays away below the threshold") {
            rulesOf(risky, failScore = 90) shouldBe listOf("BACKDOOR_PATTERN")
        }

        test("counts a rule once, however often it fires, so that a large library is not punished for its size") {
            val hits = List(50) { index -> Hit("RAW_SOCKET", "$index.class") }

            rulesOf(hits, failScore = 11) shouldBe listOf("RAW_SOCKET")
        }

        test("stops at 100") {
            val rules = listOf("BACKDOOR_PATTERN", "CRYPTO_MINING", "DOWNLOAD_AND_EXECUTE")

            val risk = findingsOf(rules.map { rule -> Hit(rule, "a") }).risk()

            risk.attributes["score"] shouldBe "100"
        }

        test("leaves out what is expected") {
            val hits = listOf(Hit("HARDCODED_URL", "a.class", "www.apache.org"))

            findingsOf(hits, failScore = 1).shouldBeEmpty()
        }
    }
})
