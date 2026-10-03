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

package com.davils.kreate.benchmark.comparison

import com.davils.kreate.benchmark.report.BenchmarkResult
import kotlin.math.abs

internal object BenchmarkComparator {
    fun compare(
        baseline: List<BenchmarkResult>,
        current: List<BenchmarkResult>,
        settings: RegressionSettings
    ): List<BenchmarkDelta> {
        val baselineByKey = baseline.associateBy { it.key }
        val currentByKey = current.associateBy { it.key }
        val keys = (baselineByKey.keys + currentByKey.keys).sorted()

        return keys.map { key -> compareOne(key, baselineByKey[key], currentByKey[key], settings) }
    }

    fun failures(deltas: List<BenchmarkDelta>, settings: RegressionSettings): List<BenchmarkDelta> =
        deltas.filter { delta -> isFailure(delta.verdict, settings) }
}

private const val PERCENT = 100.0

private fun isFailure(verdict: BenchmarkVerdict, settings: RegressionSettings): Boolean = when (verdict) {
    BenchmarkVerdict.REGRESSED, BenchmarkVerdict.INCOMPARABLE -> true
    BenchmarkVerdict.MISSING -> settings.failOnMissingBenchmark
    BenchmarkVerdict.UNCHANGED, BenchmarkVerdict.IMPROVED, BenchmarkVerdict.ADDED -> false
}

private fun compareOne(
    key: String,
    baseline: BenchmarkResult?,
    current: BenchmarkResult?,
    settings: RegressionSettings
): BenchmarkDelta {
    val threshold = thresholdFor(baseline ?: current, settings)
    val unmeasured = BenchmarkDelta(
        key = key,
        baseline = baseline,
        current = current,
        regressionPercent = Double.NaN,
        thresholdPercent = threshold,
        significant = false,
        verdict = BenchmarkVerdict.INCOMPARABLE
    )
    if (baseline == null) return unmeasured.copy(verdict = BenchmarkVerdict.ADDED)
    if (current == null) return unmeasured.copy(verdict = BenchmarkVerdict.MISSING)

    val incomparableReason = incomparableReason(baseline, current)
    if (incomparableReason != null) return unmeasured.copy(incomparableReason = incomparableReason)

    return measure(key, baseline, current, threshold, settings)
}

private fun measure(
    key: String,
    baseline: BenchmarkResult,
    current: BenchmarkResult,
    threshold: Double,
    settings: RegressionSettings
): BenchmarkDelta {
    val worseBy = worseBy(baseline, current)
    val regressionPercent = percentOf(worseBy, baseline.score)
    val significant = isSignificant(baseline, current, worseBy, settings)

    return BenchmarkDelta(
        key = key,
        baseline = baseline,
        current = current,
        regressionPercent = regressionPercent,
        thresholdPercent = threshold,
        significant = significant,
        verdict = verdictFor(regressionPercent, threshold, significant)
    )
}

private fun worseBy(baseline: BenchmarkResult, current: BenchmarkResult): Double =
    when (BenchmarkDirection.forMode(current.mode)) {
        BenchmarkDirection.HIGHER_IS_BETTER -> baseline.score - current.score
        BenchmarkDirection.LOWER_IS_BETTER -> current.score - baseline.score
    }

private fun verdictFor(regressionPercent: Double, threshold: Double, significant: Boolean): BenchmarkVerdict {
    val isRegression = regressionPercent > threshold && significant
    if (isRegression) return BenchmarkVerdict.REGRESSED

    val isImprovement = -regressionPercent > threshold
    if (isImprovement) return BenchmarkVerdict.IMPROVED

    return BenchmarkVerdict.UNCHANGED
}

private fun percentOf(worseBy: Double, baselineScore: Double): Double {
    val scale = abs(baselineScore)
    val hasScale = scale != 0.0
    if (hasScale) return worseBy / scale * PERCENT

    return unboundedPercentOf(worseBy)
}

private fun unboundedPercentOf(worseBy: Double): Double {
    if (worseBy > 0.0) return Double.POSITIVE_INFINITY
    if (worseBy < 0.0) return Double.NEGATIVE_INFINITY
    return 0.0
}

private fun isSignificant(
    baseline: BenchmarkResult,
    current: BenchmarkResult,
    worseBy: Double,
    settings: RegressionSettings
): Boolean {
    if (!settings.requireSignificance) return true

    val errors = listOf(baseline.scoreError, current.scoreError)
    val reportedErrors = errors.filter { it.isFinite() && it > 0.0 }

    return reportedErrors.isEmpty() || abs(worseBy) > reportedErrors.sum()
}

private fun incomparableReason(baseline: BenchmarkResult, current: BenchmarkResult): String? {
    if (baseline.mode != current.mode) {
        return "mode changed from '${baseline.mode}' to '${current.mode}'"
    }
    if (baseline.scoreUnit != current.scoreUnit) {
        return "unit changed from '${baseline.scoreUnit}' to '${current.scoreUnit}'"
    }
    return null
}

private fun thresholdFor(result: BenchmarkResult?, settings: RegressionSettings): Double =
    result?.benchmark
        ?.let { settings.thresholdOverrides[it] }
        ?: settings.maxRegressionPercent
