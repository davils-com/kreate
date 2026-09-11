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

package com.davils.kreate.functional

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the kotlinx-benchmark integration and its regression gate.
 *
 * Most of these never run a benchmark. The measurement itself belongs to kotlinx-benchmark;
 * what Kreate adds is the wiring around it, and that is exercised far more precisely — and
 * in a second rather than a minute — by seeding a report and excluding the execution task.
 * The one test that does run a benchmark is tagged `slow`.
 */
@DisplayName("Benchmarks")
class BenchmarkFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    // No version: the plugin is on the injected TestKit classpath, the same way the Kotlin
    // plugin is, so that both share a classloader.
    private val benchmarkPlugin = """id("org.jetbrains.kotlinx.benchmark")"""

    /** The execution task kotlinx-benchmark creates for the `benchmarks` target, `main` profile. */
    private val executionTask = "benchmarksBenchmark"

    private val baseline: File get() = fixture.file("benchmarks/baseline.json")

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample {
                fun value(): Int = 40 + 2
            }
            """.trimIndent()
        )
    }

    private fun writeBuild(
        benchmarkBlock: String = "enabled = true",
        withPlugin: Boolean = true
    ) {
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    benchmark {
                        $benchmarkBlock
                    }
                }
            """.trimIndent(),
            extraPlugins = if (withPlugin) listOf(benchmarkPlugin) else emptyList()
        )
    }

    /**
     * Writes a report where kotlinx-benchmark would have left one, under a timestamped
     * directory of the given name.
     */
    private fun seedReport(score: Double, timestamp: String = "2026-08-19T10.00.00", error: Double = 1.0) {
        fixture.write(
            "build/reports/benchmarks/main/$timestamp/benchmarks.json",
            """
            [
              {
                "benchmark" : "com.example.SampleBenchmark.value",
                "mode" : "thrpt",
                "params" : { },
                "primaryMetric" : {
                   "score": $score,
                   "scoreError": $error,
                   "scoreUnit" : "ops/s"
                }
              }
            ]
            """.trimIndent()
        )
    }

    private fun build(vararg tasks: String) =
        fixture.build(*tasks, "-x", executionTask)

    private fun buildAndFail(vararg tasks: String) =
        fixture.buildAndFail(*tasks, "-x", executionTask)

    @Test
    @DisplayName("registers no tasks while the feature is disabled")
    fun registersNothingWhenDisabled() {
        writeBuild("enabled = false")

        val result = fixture.build("tasks", "--all")

        result.output shouldNotContain "kreateBenchmarkCheck"
        result.output shouldNotContain "kreateBenchmarkBaseline"
    }

    @Test
    @DisplayName("applies the kotlinx-benchmark plugin itself")
    fun appliesItsOwnPlugin() {
        // Nothing in the generated build script mentions kotlinx-benchmark. Enabling the
        // feature is the decision; applying its plugin is bookkeeping Kreate does.
        writeBuild(withPlugin = false)

        val result = fixture.build("tasks", "--all")

        result.output shouldContain "benchmarksBenchmarkGenerate"
        result.output shouldContain "kreateBenchmarkCheck"
    }

    @Test
    @DisplayName("normalizes the newest run to a stable path")
    fun normalizesNewestRun() {
        writeBuild()
        seedReport(score = 100.0, timestamp = "2026-08-19T09.00.00")
        Thread.sleep(FILE_TIMESTAMP_RESOLUTION_MS)
        seedReport(score = 200.0, timestamp = "2026-08-19T10.00.00")

        val result = build("kreateBenchmarkReport")

        result.task(":kreateBenchmarkReport")?.outcome shouldBe TaskOutcome.SUCCESS
        val normalized = fixture.file("build/reports/kreate/benchmark/main/benchmarks.json")
        normalized.readText() shouldContain "200.0"
        normalized.readText() shouldNotContain "100.0"
    }

    @Test
    @DisplayName("records a baseline from the normalized report")
    fun recordsBaseline() {
        writeBuild()
        seedReport(score = 1000.0)

        val result = build("kreateBenchmarkBaseline")

        result.task(":kreateBenchmarkBaseline")?.outcome shouldBe TaskOutcome.SUCCESS
        baseline.readText() shouldContain "com.example.SampleBenchmark.value"
    }

    @Test
    @DisplayName("passes the check against a freshly recorded baseline")
    fun checkPassesAgainstFreshBaseline() {
        writeBuild()
        seedReport(score = 1000.0)
        build("kreateBenchmarkBaseline")

        val result = build("kreateBenchmarkCheck")

        result.task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
        fixture.file("build/reports/kreate/benchmark/comparison.md")
            .readText() shouldContain "unchanged"
    }

    @Test
    @DisplayName("fails the check and names the baseline task when a benchmark regressed")
    fun checkFailsOnRegression() {
        writeBuild()
        seedReport(score = 1000.0)
        build("kreateBenchmarkBaseline")

        seedReport(score = 500.0, timestamp = "2026-08-19T11.00.00")
        val result = buildAndFail("kreateBenchmarkCheck")

        result.output shouldContain "Benchmark regression in project ':'"
        result.output shouldContain "com.example.SampleBenchmark.value"
        result.output shouldContain "./gradlew :kreateBenchmarkBaseline"
    }

    @Test
    @DisplayName("passes when a drop is smaller than the measurement error")
    fun checkToleratesNoise() {
        writeBuild()
        seedReport(score = 1000.0, error = 300.0)
        build("kreateBenchmarkBaseline")

        // 200 apart with 600 of combined error: on a shared runner this is ordinary variance,
        // and a gate that fires here gets switched off within a week.
        seedReport(score = 800.0, timestamp = "2026-08-19T11.00.00", error = 300.0)
        val result = build("kreateBenchmarkCheck")

        result.task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    @DisplayName("explains that no baseline has been recorded yet")
    fun checkExplainsMissingBaseline() {
        writeBuild()
        seedReport(score = 1000.0)

        val result = buildAndFail("kreateBenchmarkCheck")

        result.output shouldContain "No benchmark baseline has been recorded"
        result.output shouldContain "./gradlew :kreateBenchmarkBaseline"
    }

    @Test
    @DisplayName("fails when a benchmark disappeared from the run")
    fun checkFailsOnMissingBenchmark() {
        writeBuild()
        seedReport(score = 1000.0)
        build("kreateBenchmarkBaseline")

        fixture.write("build/reports/benchmarks/main/2026-08-19T11.00.00/benchmarks.json", "[]")
        val result = buildAndFail("kreateBenchmarkCheck")

        result.output shouldContain "in the baseline but not in this run"
    }

    @Test
    @DisplayName("refuses a gate profile that cannot produce a readable report")
    fun rejectsNonJsonGateProfile() {
        writeBuild(
            """
            enabled = true
            profiles {
                named("main") {
                    reportFormat = "csv"
                }
            }
            """.trimIndent()
        )

        val result = fixture.buildAndFail("tasks")

        result.output shouldContain "can only read 'json'"
    }

    @Test
    @DisplayName("warns when the threshold can never be reached")
    fun warnsAboutUnreachableThreshold() {
        writeBuild(
            """
            enabled = true
            regression {
                maxRegressionPercent = 150.0
            }
            """.trimIndent()
        )

        val result = fixture.build("tasks")

        // In throughput mode a score cannot drop by more than 100%, so this configuration
        // switches the gate off while still looking like a passing build.
        result.output shouldContain "cannot drop by more than"
    }

    @Test
    @DisplayName("reuses the configuration cache for the gate")
    fun reusesConfigurationCache() {
        writeBuild()
        seedReport(score = 1000.0)
        build("kreateBenchmarkBaseline")
        build("kreateBenchmarkCheck")

        val result = build("kreateBenchmarkCheck")

        result.output shouldContain "Configuration cache entry reused"
    }

    @Test
    @Tag("slow")
    @DisplayName("runs a real benchmark end to end and compares it")
    fun runsBenchmarkEndToEnd() {
        writeBuild(
            """
            enabled = true
            profiles {
                named("main") {
                    warmups = 0
                    iterations = 1
                    iterationTime = 100
                    iterationTimeUnit = "ms"
                    advanced("jvmForks", "1")
                }
            }
            regression {
                // A single 100 ms iteration measures almost nothing, so the gate is opened
                // wide: this test is about the pipeline working, not about the number.
                maxRegressionPercent = 1000.0
            }
            """.trimIndent()
        )
        fixture.write(
            "src/benchmarks/kotlin/com/example/SampleBenchmark.kt",
            """
            package com.example

            import kotlinx.benchmark.Benchmark
            import kotlinx.benchmark.Scope
            import kotlinx.benchmark.State

            @State(Scope.Benchmark)
            class SampleBenchmark {
                private val sample = Sample()

                @Benchmark
                fun value(): Int = sample.value()
            }
            """.trimIndent()
        )

        // Proves what no seeded report can: the source set is associated with `main` (the
        // benchmark calls into it), allopen opened the @State class for JMH, and the report
        // lands where the normalization task looks for it.
        val result = fixture.build("kreateBenchmarkBaseline")

        result.task(":$executionTask")?.outcome shouldBe TaskOutcome.SUCCESS
        baseline.readText() shouldContain "com.example.SampleBenchmark.value"

        fixture.build("kreateBenchmarkCheck")
            .task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    private companion object {
        /** Enough for two runs to differ in modification time on any file system. */
        const val FILE_TIMESTAMP_RESOLUTION_MS = 1100L
    }
}
