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
import java.util.Locale

internal object ComparisonRenderer {
    fun renderMarkdown(deltas: List<BenchmarkDelta>, profile: String): String = buildString {
        appendLine("# Benchmark comparison — profile `$profile`")
        appendLine()
        appendBody(deltas)
    }

    fun renderFailures(failures: List<BenchmarkDelta>): String = buildString {
        failures.take(MAX_REPORTED_FAILURES).forEach { delta ->
            appendLine("  ${describeFailure(delta)}")
        }
        if (failures.size > MAX_REPORTED_FAILURES) {
            appendLine("  ... and ${failures.size - MAX_REPORTED_FAILURES} more")
        }
    }.trimEnd()
}

private const val MAX_REPORTED_FAILURES = 25

private const val NO_VALUE = "—"

private fun StringBuilder.appendBody(deltas: List<BenchmarkDelta>) {
    if (deltas.isEmpty()) {
        appendLine("No benchmarks were measured.")
        return
    }
    appendTable(deltas)
}

private fun StringBuilder.appendTable(deltas: List<BenchmarkDelta>) {
    appendLine("| Benchmark | Baseline | Current | Unit | Change | Limit | Verdict |")
    appendLine("| --- | ---: | ---: | --- | ---: | ---: | --- |")
    deltas.forEach { delta ->
        appendLine(
            "| `${delta.key}` " +
                "| ${score(delta.baseline)} " +
                "| ${score(delta.current)} " +
                "| ${unit(delta)} " +
                "| ${change(delta)} " +
                "| ${percent(delta.thresholdPercent)} " +
                "| ${verdict(delta)}${incomparableSuffix(delta)} |"
        )
    }
    appendLine()
    appendLine(
        "Change is stated in the worse direction: a positive value is a regression, " +
            "a negative value an improvement."
    )
}

private fun describeFailure(delta: BenchmarkDelta): String = when (delta.verdict) {
    BenchmarkVerdict.MISSING -> {
        "${delta.key}: in the baseline but not in this run"
    }
    BenchmarkVerdict.INCOMPARABLE -> {
        "${delta.key}: cannot be compared, ${delta.incomparableReason}"
    }
    BenchmarkVerdict.REGRESSED,
    BenchmarkVerdict.UNCHANGED,
    BenchmarkVerdict.IMPROVED,
    BenchmarkVerdict.ADDED -> {
        "${delta.key}: ${score(delta.baseline)} -> ${score(delta.current)} ${unit(delta)}, " +
            "${percent(delta.regressionPercent)} worse (limit ${percent(delta.thresholdPercent)})"
    }
}

private fun verdict(delta: BenchmarkDelta): String = when (delta.verdict) {
    BenchmarkVerdict.UNCHANGED -> "unchanged"
    BenchmarkVerdict.IMPROVED -> "improved"
    BenchmarkVerdict.REGRESSED -> "**regressed**"
    BenchmarkVerdict.MISSING -> "**missing**"
    BenchmarkVerdict.ADDED -> "new"
    BenchmarkVerdict.INCOMPARABLE -> "**incomparable**"
}

private fun change(delta: BenchmarkDelta): String {
    if (delta.regressionPercent.isNaN()) return NO_VALUE

    val isWithinMeasurementError = delta.verdict != BenchmarkVerdict.REGRESSED && !delta.significant
    if (isWithinMeasurementError) return "${percent(delta.regressionPercent)} (noise)"

    return percent(delta.regressionPercent)
}

private fun incomparableSuffix(delta: BenchmarkDelta): String =
    delta.incomparableReason?.let { " ($it)" }.orEmpty()

private fun score(result: BenchmarkResult?): String =
    result?.let { format(it.score) } ?: NO_VALUE

private fun unit(delta: BenchmarkDelta): String =
    delta.current?.scoreUnit ?: delta.baseline?.scoreUnit ?: ""

private fun percent(value: Double): String {
    if (value.isNaN()) return NO_VALUE
    if (value == Double.POSITIVE_INFINITY) return "+INF%"
    if (value == Double.NEGATIVE_INFINITY) return "-INF%"
    return String.format(Locale.ROOT, "%+.1f%%", value)
}

private fun format(value: Double): String = String.format(Locale.ROOT, "%.3f", value)
