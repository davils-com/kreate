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
import com.davils.kreate.assurance.finding.isAtLeast
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.governance.policy.DeclaredDependency
import com.davils.kreate.governance.policy.ModulePolicy
import com.davils.kreate.provenance.typosquat.TyposquatDetector

internal class DependencyAdder(
    private val advisories: (ModuleCoordinate) -> FindingDocument,
    private val trust: (ModuleCoordinate) -> FindingDocument,
    private val typosquat: TyposquatDetector,
    private val policy: ModulePolicy
) {
    fun decide(module: ModuleCoordinate): AddDecision {
        val declared = DeclaredDependency("implementation", module.group, module.name, module.version, module.version)
        val typosquatted = typosquat.findingsOf(listOf(module))
        val denied = policy.deniedOf(listOf(module))
        val local = typosquatted + denied + policy.notAllowedOf(listOf(declared))
        val documents = listOf(advisories(module), trust(module))
        val findings = local + documents.flatMap { document -> document.findings }
        val incomplete = documents.firstOrNull { document -> document.status == DocumentStatus.INCOMPLETE }
        val reasons = findings.mapNotNull { finding -> blockReasonOf(finding) }
        return AddDecision(module, findings, reasons.distinct(), incomplete?.let { document -> reasonOf(document) })
    }

    private fun reasonOf(document: FindingDocument): String =
        "${document.source.id}: ${document.reason ?: "the check did not complete"}"

    private fun blockReasonOf(finding: Finding): String? {
        val severity = finding.gatingSeverity
        val threshold = thresholdOf(finding.source.id)
        if (!severity.isAtLeast(threshold)) return null
        return "${finding.ruleId}: ${finding.title}"
    }

    private fun thresholdOf(source: String): Severity {
        if (source == TRUST_SOURCE) return Severity.CRITICAL
        return Severity.HIGH
    }

    private companion object {
        const val TRUST_SOURCE: String = "trust"
    }
}
