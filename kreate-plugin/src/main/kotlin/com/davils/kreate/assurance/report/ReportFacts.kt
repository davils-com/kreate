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
package com.davils.kreate.assurance.report

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.finding.Finding

private const val BUILD_FILE: String = "build.gradle.kts"

internal object ReportFacts {
    fun placeOf(finding: Finding): String = finding.location?.path ?: finding.module ?: BUILD_FILE

    fun filePathOf(finding: Finding): String = finding.location?.path ?: BUILD_FILE

    fun lineOf(finding: Finding): Int = finding.location?.line ?: 1

    fun countsOf(findings: List<Finding>): Map<Severity, Int> {
        val counts = findings.groupingBy { finding -> finding.gatingSeverity }.eachCount()
        return Severity.entries.associateWith { severity -> counts[severity] ?: 0 }
    }

    fun ordered(findings: List<Finding>): List<Finding> =
        findings.sortedWith(compareBy({ finding -> finding.gatingSeverity }, { finding -> finding.ruleId }))
}
