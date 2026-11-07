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
package com.davils.kreate.inspection.scan

import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.rule.RuleInfo

private const val MAX_SCORE: Int = 100

private val CODE_WEIGHTS: List<Int> = listOf(40, 20, 10, 5, 5)

private val ENTROPY_WEIGHTS: List<Int> = listOf(0, 15, 5, 0, 0)

private val UNICODE_WEIGHTS: List<Int> = listOf(35, 15, 0, 0, 0)

private val ARCHIVE_WEIGHTS: List<Int> = listOf(30, 10, 5, 0, 0)

private val BUILD_TIME_WEIGHTS: List<Int> = listOf(30, 10, 3, 0, 0)

private val CATEGORY_WEIGHTS: Map<String, List<Int>> = mapOf(
    "code" to CODE_WEIGHTS,
    "entropy" to ENTROPY_WEIGHTS,
    "unicode" to UNICODE_WEIGHTS,
    "archive" to ARCHIVE_WEIGHTS,
    "buildtime" to BUILD_TIME_WEIGHTS,
    "pom" to BUILD_TIME_WEIGHTS
)

internal object RiskScorer {
    fun scoreOf(rules: Collection<RuleInfo>): Int {
        val total = rules.distinct().sumOf { rule -> weightOf(rule) }
        return minOf(total, MAX_SCORE)
    }

    private fun weightOf(rule: RuleInfo): Int {
        val weights = CATEGORY_WEIGHTS[rule.category] ?: return 0
        return weights[severityIndex(rule.severity)]
    }

    private fun severityIndex(severity: Severity): Int = severity.ordinal
}
