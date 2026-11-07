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
package com.davils.kreate.policy.check

import com.davils.kreate.assurance.Severity
import java.time.Duration
import java.time.format.DateTimeParseException

internal enum class Strictness {
    LESS_SEVERE,
    LOWER_NUMBER,
    HIGHER_NUMBER,
    SHORTER_DURATION;

    fun isAtLeastAsStrict(actual: String, minimum: String): Boolean? = when (this) {
        LESS_SEVERE -> compare(severityRank(actual), severityRank(minimum)) { value, limit -> value >= limit }
        LOWER_NUMBER -> compare(numberOf(actual), numberOf(minimum)) { value, limit -> value <= limit }
        HIGHER_NUMBER -> compare(numberOf(actual), numberOf(minimum)) { value, limit -> value >= limit }
        SHORTER_DURATION -> compare(secondsOf(actual), secondsOf(minimum)) { value, limit -> value <= limit }
    }

    private fun compare(actual: Double?, minimum: Double?, check: (Double, Double) -> Boolean): Boolean? {
        if (actual == null || minimum == null) return null
        return check(actual, minimum)
    }

    private fun severityRank(text: String): Double? =
        Severity.entries.firstOrNull { severity -> severity.name == text.uppercase() }?.ordinal?.toDouble()

    private fun numberOf(text: String): Double? = text.toDoubleOrNull()

    private fun secondsOf(text: String): Double? = try {
        Duration.parse(text).seconds.toDouble()
    } catch (_: DateTimeParseException) {
        null
    }

    companion object {
        val OF_OPTION: Map<String, Strictness> = mapOf(
            "failOn" to LESS_SEVERE,
            "minSeverity" to LESS_SEVERE,
            "baseline.maxSeverity" to LESS_SEVERE,
            "maxAdvisoryAge" to SHORTER_DURATION,
            "reuseVerdictFor" to SHORTER_DURATION,
            "inspection.failScore" to LOWER_NUMBER,
            "trust.freshPackageDays" to HIGHER_NUMBER,
            "trust.youngPackageDays" to HIGHER_NUMBER,
            "trust.freshReleaseHours" to HIGHER_NUMBER,
            "trust.minScorecard" to HIGHER_NUMBER,
            "dependencyPolicy.maxMajorBehind" to LOWER_NUMBER,
            "dependencyPolicy.maxLibyears" to LOWER_NUMBER
        )
    }
}
