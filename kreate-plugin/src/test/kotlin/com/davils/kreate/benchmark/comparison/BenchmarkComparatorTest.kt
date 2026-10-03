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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BenchmarkComparatorTest : FunSpec({
    val lenient = RegressionSettings(
        maxRegressionPercent = 10.0,
        requireSignificance = false
    )

    fun result(
        name: String = "com.example.Bench.run",
        mode: String = "thrpt",
        score: Double,
        error: Double = 0.0,
        unit: String = "ops/s"
    ) = BenchmarkResult(
        benchmark = name,
        mode = mode,
        params = emptyMap(),
        score = score,
        scoreError = error,
        scoreUnit = unit
    )

    fun verdictOf(
        baseline: BenchmarkResult,
        current: BenchmarkResult,
        settings: RegressionSettings = lenient
    ) = BenchmarkComparator.compare(listOf(baseline), listOf(current), settings).single()

    context("Benchmark comparison") {
        test("treats a lower throughput score as a regression") {
            val delta = verdictOf(result(score = 1000.0), result(score = 800.0))

            delta.verdict shouldBe BenchmarkVerdict.REGRESSED
            delta.regressionPercent shouldBe 20.0
        }

        test("treats a higher throughput score as an improvement") {
            verdictOf(result(score = 1000.0), result(score = 1300.0))
                .verdict shouldBe BenchmarkVerdict.IMPROVED
        }

        test("reverses the direction for a time based mode") {
            val slowerIsRegressionForAvgt = verdictOf(
                result(mode = "avgt", score = 100.0, unit = "ns/op"),
                result(mode = "avgt", score = 130.0, unit = "ns/op")
            )
            slowerIsRegressionForAvgt.verdict shouldBe BenchmarkVerdict.REGRESSED
            slowerIsRegressionForAvgt.regressionPercent shouldBe 30.0

            verdictOf(
                result(mode = "avgt", score = 100.0, unit = "ns/op"),
                result(mode = "avgt", score = 70.0, unit = "ns/op")
            ).verdict shouldBe BenchmarkVerdict.IMPROVED
        }

        test("passes a movement inside the threshold") {
            verdictOf(result(score = 1000.0), result(score = 950.0))
                .verdict shouldBe BenchmarkVerdict.UNCHANGED
        }

        test("ignores a regression that is smaller than the measurement error") {
            val strict = RegressionSettings(maxRegressionPercent = 10.0, requireSignificance = true)

            val dropInsideCombinedErrorMargins = verdictOf(
                result(score = 1000.0, error = 150.0),
                result(score = 800.0, error = 100.0),
                strict
            )

            dropInsideCombinedErrorMargins.verdict shouldBe BenchmarkVerdict.UNCHANGED
            dropInsideCombinedErrorMargins.significant shouldBe false
        }

        test("reports a regression that exceeds the measurement error") {
            val strict = RegressionSettings(maxRegressionPercent = 10.0, requireSignificance = true)

            val delta = verdictOf(
                result(score = 1000.0, error = 10.0),
                result(score = 800.0, error = 10.0),
                strict
            )

            delta.verdict shouldBe BenchmarkVerdict.REGRESSED
            delta.significant shouldBe true
        }

        test("lets the threshold decide when a run reports no error margin") {
            val strict = RegressionSettings(maxRegressionPercent = 10.0, requireSignificance = true)

            verdictOf(
                result(score = 1000.0, error = Double.NaN),
                result(score = 800.0, error = Double.NaN),
                strict
            ).verdict shouldBe BenchmarkVerdict.REGRESSED
        }

        test("applies a per-benchmark threshold override") {
            val settings = RegressionSettings(
                maxRegressionPercent = 10.0,
                thresholdOverrides = mapOf("com.example.Bench.run" to 50.0),
                requireSignificance = false
            )

            val delta = verdictOf(result(score = 1000.0), result(score = 800.0), settings)

            delta.verdict shouldBe BenchmarkVerdict.UNCHANGED
            delta.thresholdPercent shouldBe 50.0
        }

        test("refuses to compare measurements in different units") {
            val delta = verdictOf(
                result(score = 1000.0, unit = "ops/s"),
                result(score = 1000.0, unit = "ops/ms")
            )

            delta.verdict shouldBe BenchmarkVerdict.INCOMPARABLE
            delta.incomparableReason.orEmpty() shouldContain "unit changed"
        }

        test("refuses to compare measurements taken in different modes") {
            val delta = verdictOf(
                result(mode = "thrpt", score = 1000.0),
                result(mode = "avgt", score = 1000.0)
            )

            delta.verdict shouldBe BenchmarkVerdict.INCOMPARABLE
            delta.incomparableReason.orEmpty() shouldContain "mode changed"
        }

        test("flags a benchmark that vanished from the run") {
            val deltas = BenchmarkComparator.compare(listOf(result(score = 1000.0)), emptyList(), lenient)

            deltas.single().verdict shouldBe BenchmarkVerdict.MISSING
            BenchmarkComparator.failures(deltas, lenient) shouldHaveSize 1
            BenchmarkComparator.failures(
                deltas,
                lenient.copy(failOnMissingBenchmark = false)
            ).shouldBeEmpty()
        }

        test("reports a new benchmark without failing") {
            val deltas = BenchmarkComparator.compare(emptyList(), listOf(result(score = 1000.0)), lenient)

            deltas.single().verdict shouldBe BenchmarkVerdict.ADDED
            BenchmarkComparator.failures(deltas, lenient).shouldBeEmpty()
        }

        test("compares parameterised runs separately") {
            val small = result(score = 1000.0).copy(params = mapOf("size" to "1"))
            val large = result(score = 10.0).copy(params = mapOf("size" to "1000"))

            val deltas = BenchmarkComparator.compare(
                listOf(small, large),
                listOf(small, large.copy(score = 5.0)),
                lenient
            )

            deltas shouldHaveSize 2
            deltas.first { it.key.contains("size=1]") }.verdict shouldBe BenchmarkVerdict.UNCHANGED
            deltas.first { it.key.contains("size=1000]") }.verdict shouldBe BenchmarkVerdict.REGRESSED
        }

        test("does not divide by zero when the baseline score was zero") {
            val delta = verdictOf(result(score = 0.0), result(mode = "avgt", score = 0.0))

            delta.verdict shouldBe BenchmarkVerdict.INCOMPARABLE

            val sameMode = verdictOf(result(mode = "avgt", score = 0.0), result(mode = "avgt", score = 5.0))
            sameMode.regressionPercent shouldBe Double.POSITIVE_INFINITY
            sameMode.verdict shouldBe BenchmarkVerdict.REGRESSED
        }
    }
})
