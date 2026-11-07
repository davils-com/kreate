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
package com.davils.kreate.governance.add

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.governance.policy.ModulePolicy
import com.davils.kreate.governance.policy.PolicySettings
import com.davils.kreate.provenance.typosquat.TyposquatDetector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant

private val MODULE: ModuleCoordinate = ModuleCoordinate("org.example", "lib", "1.0.0")

private fun documentOf(source: FindingSource, vararg findings: Finding, complete: Boolean = true): FindingDocument {
    if (!complete) return FindingDocument(source, DocumentStatus.INCOMPLETE, Instant.EPOCH, findings.toList(), "down")
    return FindingDocument(source, DocumentStatus.COMPLETE, Instant.EPOCH, findings.toList())
}

private fun findingOf(source: FindingSource, severity: Severity) =
    Finding("RULE-1", source, severity, "A problem", module = MODULE.module, version = MODULE.version)

private fun reasonsOf(adder: DependencyAdder, module: ModuleCoordinate = MODULE): List<String> =
    adder.decide(module).blockReasons

private fun adder(
    advisories: FindingDocument = documentOf(FindingSource.OSV),
    trust: FindingDocument = documentOf(FindingSource.TRUST),
    settings: PolicySettings = PolicySettings()
): DependencyAdder = DependencyAdder(
    advisories = { advisories },
    trust = { trust },
    typosquat = TyposquatDetector(setOf("com.google.guava:guava"), emptySet()),
    policy = ModulePolicy(settings)
)

class DependencyAdderTest : FunSpec({
    context("a module") {
        test("without findings may be added") {
            val decision = adder().decide(MODULE)

            decision.blockReasons.shouldBeEmpty()
            decision.incompleteReason.shouldBeNull()
        }

        test("with a high advisory is blocked") {
            val advisories = documentOf(FindingSource.OSV, findingOf(FindingSource.OSV, Severity.HIGH))

            reasonsOf(adder(advisories = advisories)).single() shouldContain "RULE-1"
        }

        test("with a medium trust finding is not blocked, a critical one is") {
            val medium = documentOf(FindingSource.TRUST, findingOf(FindingSource.TRUST, Severity.MEDIUM))
            val critical = documentOf(FindingSource.TRUST, findingOf(FindingSource.TRUST, Severity.CRITICAL))

            reasonsOf(adder(trust = medium)).shouldBeEmpty()
            reasonsOf(adder(trust = critical)).size shouldBe 1
        }

        test("that looks like a typosquat is blocked") {
            val decision = adder().decide(ModuleCoordinate("com.goog1e.guava", "guava", "1.0"))

            decision.blockReasons.single() shouldContain "TYPOSQUAT_GROUP"
        }

        test("that the policy denies is blocked") {
            val settings = PolicySettings(denied = mapOf("org.example" to "Not here."))

            reasonsOf(adder(settings = settings)).single() shouldContain "DENIED_MODULE"
        }

        test("that could not be checked is incomplete") {
            val decision = adder(advisories = documentOf(FindingSource.OSV, complete = false)).decide(MODULE)

            decision.incompleteReason shouldBe "osv: down"
        }
    }
})
