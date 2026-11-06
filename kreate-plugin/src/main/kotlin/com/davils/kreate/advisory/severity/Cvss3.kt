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
package com.davils.kreate.advisory.severity

import com.davils.kreate.assurance.Severity
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val VECTOR_PREFIX: String = "CVSS:3"

private const val SEVERITY_CRITICAL_FROM: Double = 9.0

private const val SEVERITY_HIGH_FROM: Double = 7.0

private const val SEVERITY_MEDIUM_FROM: Double = 4.0

private const val MAX_SCORE: Double = 10.0

private const val EXPLOITABILITY_FACTOR: Double = 8.22

private const val UNCHANGED_IMPACT_FACTOR: Double = 6.42

private const val CHANGED_IMPACT_FACTOR: Double = 7.52

private const val CHANGED_IMPACT_OFFSET: Double = 0.029

private const val CHANGED_IMPACT_POWER_FACTOR: Double = 3.25

private const val CHANGED_IMPACT_POWER_OFFSET: Double = 0.02

private const val CHANGED_IMPACT_POWER: Double = 15.0

private const val CHANGED_SCOPE_FACTOR: Double = 1.08

private const val ROUNDING_SCALE: Double = 100_000.0

private const val ROUNDING_STEP: Int = 10_000

private const val ONE_DECIMAL: Double = 10.0

private const val ATTACK_NETWORK: Double = 0.85

private const val ATTACK_ADJACENT: Double = 0.62

private const val ATTACK_LOCAL: Double = 0.55

private const val ATTACK_PHYSICAL: Double = 0.2

private const val COMPLEXITY_LOW: Double = 0.77

private const val COMPLEXITY_HIGH: Double = 0.44

private const val PRIVILEGES_NONE: Double = 0.85

private const val PRIVILEGES_LOW: Double = 0.62

private const val PRIVILEGES_LOW_CHANGED: Double = 0.68

private const val PRIVILEGES_HIGH: Double = 0.27

private const val PRIVILEGES_HIGH_CHANGED: Double = 0.5

private const val INTERACTION_NONE: Double = 0.85

private const val INTERACTION_REQUIRED: Double = 0.62

private const val IMPACT_HIGH: Double = 0.56

private const val IMPACT_LOW: Double = 0.22

private const val IMPACT_NONE: Double = 0.0

private const val SCOPE_CHANGED: String = "C"

private val ATTACK_VECTOR: Map<String, Double> = mapOf(
    "N" to ATTACK_NETWORK,
    "A" to ATTACK_ADJACENT,
    "L" to ATTACK_LOCAL,
    "P" to ATTACK_PHYSICAL
)

private val ATTACK_COMPLEXITY: Map<String, Double> = mapOf("L" to COMPLEXITY_LOW, "H" to COMPLEXITY_HIGH)

private val PRIVILEGES_UNCHANGED: Map<String, Double> =
    mapOf("N" to PRIVILEGES_NONE, "L" to PRIVILEGES_LOW, "H" to PRIVILEGES_HIGH)

private val PRIVILEGES_CHANGED: Map<String, Double> =
    mapOf("N" to PRIVILEGES_NONE, "L" to PRIVILEGES_LOW_CHANGED, "H" to PRIVILEGES_HIGH_CHANGED)

private val USER_INTERACTION: Map<String, Double> = mapOf("N" to INTERACTION_NONE, "R" to INTERACTION_REQUIRED)

private val IMPACT: Map<String, Double> = mapOf("H" to IMPACT_HIGH, "L" to IMPACT_LOW, "N" to IMPACT_NONE)

internal object Cvss3 {
    fun severityOf(vector: String): Severity? {
        val score = scoreOf(vector) ?: return null
        if (score >= SEVERITY_CRITICAL_FROM) return Severity.CRITICAL
        if (score >= SEVERITY_HIGH_FROM) return Severity.HIGH
        if (score >= SEVERITY_MEDIUM_FROM) return Severity.MEDIUM
        return Severity.LOW
    }

    fun scoreOf(vector: String): Double? {
        if (!vector.startsWith(VECTOR_PREFIX)) return null

        val metrics = metricsOf(vector)
        val isScopeChanged = metrics["S"] == SCOPE_CHANGED
        val impact = impactOf(metrics, isScopeChanged) ?: return null
        val exploitability = exploitabilityOf(metrics, isScopeChanged) ?: return null
        if (impact <= 0.0) return 0.0

        val base = impact + exploitability
        if (isScopeChanged) return roundUp(min(CHANGED_SCOPE_FACTOR * base, MAX_SCORE))
        return roundUp(min(base, MAX_SCORE))
    }

    private fun metricsOf(vector: String): Map<String, String> {
        val metrics = vector.split('/').drop(1)
        return metrics.associate { metric -> metric.substringBefore(':') to metric.substringAfter(':') }
    }

    private fun impactOf(metrics: Map<String, String>, isScopeChanged: Boolean): Double? {
        val confidentiality = IMPACT[metrics["C"]] ?: return null
        val integrity = IMPACT[metrics["I"]] ?: return null
        val availability = IMPACT[metrics["A"]] ?: return null
        val subScore = 1 - (1 - confidentiality) * (1 - integrity) * (1 - availability)

        if (!isScopeChanged) return UNCHANGED_IMPACT_FACTOR * subScore

        val power = (subScore - CHANGED_IMPACT_POWER_OFFSET).pow(CHANGED_IMPACT_POWER)
        return CHANGED_IMPACT_FACTOR * (subScore - CHANGED_IMPACT_OFFSET) - CHANGED_IMPACT_POWER_FACTOR * power
    }

    private fun exploitabilityOf(metrics: Map<String, String>, isScopeChanged: Boolean): Double? {
        val privileges = privilegesFor(isScopeChanged)
        val attackVector = ATTACK_VECTOR[metrics["AV"]] ?: return null
        val complexity = ATTACK_COMPLEXITY[metrics["AC"]] ?: return null
        val required = privileges[metrics["PR"]] ?: return null
        val interaction = USER_INTERACTION[metrics["UI"]] ?: return null
        return EXPLOITABILITY_FACTOR * attackVector * complexity * required * interaction
    }

    private fun privilegesFor(isScopeChanged: Boolean): Map<String, Double> {
        if (isScopeChanged) return PRIVILEGES_CHANGED
        return PRIVILEGES_UNCHANGED
    }

    private fun roundUp(score: Double): Double {
        val scaled = (score * ROUNDING_SCALE).roundToInt()
        if (scaled % ROUNDING_STEP == 0) return scaled / ROUNDING_SCALE
        return ceil(scaled.toDouble() / ROUNDING_STEP) / ONE_DECIMAL
    }
}
