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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

private const val BENCHMARK_PLUGIN_FROM_TESTKIT_CLASSPATH: String = """id("org.jetbrains.kotlinx.benchmark")"""

private const val MAIN_PROFILE_EXECUTION_TASK: String = "benchmarksBenchmark"

private const val FILE_TIMESTAMP_RESOLUTION_MS: Long = 1100L

private const val DEFAULT_REPORT_TIMESTAMP: String = "2026-08-19T10.00.00"

private const val DEFAULT_SCORE_ERROR: Double = 1.0

class BenchmarkFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
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
        return fixture
    }

    context("Benchmarks") {
        test("registers no tasks while the feature is disabled") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild("enabled = false")

            val result = fixture.build("tasks", "--all")

            result.output shouldNotContain "kreateBenchmarkCheck"
            result.output shouldNotContain "kreateBenchmarkBaseline"
        }

        test("applies the kotlinx-benchmark plugin itself") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild(isPluginApplied = false)

            val result = fixture.build("tasks", "--all")

            result.output shouldContain "benchmarksBenchmarkGenerate"
            result.output shouldContain "kreateBenchmarkCheck"
        }

        test("normalizes the newest run to a stable path") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 100.0, timestamp = "2026-08-19T09.00.00")
            Thread.sleep(FILE_TIMESTAMP_RESOLUTION_MS)
            fixture.seedReport(score = 200.0, timestamp = "2026-08-19T10.00.00")

            val result = fixture.buildWithoutExecution("kreateBenchmarkReport")

            result.task(":kreateBenchmarkReport")?.outcome shouldBe TaskOutcome.SUCCESS
            val normalized = fixture.file("build/reports/kreate/benchmark/main/benchmarks.json")
            normalized.readText() shouldContain "200.0"
            normalized.readText() shouldNotContain "100.0"
        }

        test("records a baseline from the normalized report") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)

            val result = fixture.buildWithoutExecution("kreateBenchmarkBaseline")

            result.task(":kreateBenchmarkBaseline")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.baseline().readText() shouldContain "com.example.SampleBenchmark.value"
        }

        test("passes the check against a freshly recorded baseline") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)
            fixture.buildWithoutExecution("kreateBenchmarkBaseline")

            val result = fixture.buildWithoutExecution("kreateBenchmarkCheck")

            result.task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.file("build/reports/kreate/benchmark/comparison.md").readText() shouldContain "unchanged"
        }

        test("fails the check and names the baseline task when a benchmark regressed") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)
            fixture.buildWithoutExecution("kreateBenchmarkBaseline")

            fixture.seedReport(score = 500.0, timestamp = "2026-08-19T11.00.00")
            val result = fixture.buildAndFailWithoutExecution("kreateBenchmarkCheck")

            result.output shouldContain "Benchmark regression in project ':'"
            result.output shouldContain "com.example.SampleBenchmark.value"
            result.output shouldContain "./gradlew :kreateBenchmarkBaseline"
        }

        test("passes when a drop is smaller than the measurement error") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            val sharedRunnerVariance = 300.0
            fixture.seedReport(score = 1000.0, error = sharedRunnerVariance)
            fixture.buildWithoutExecution("kreateBenchmarkBaseline")

            fixture.seedReport(score = 800.0, timestamp = "2026-08-19T11.00.00", error = sharedRunnerVariance)
            val result = fixture.buildWithoutExecution("kreateBenchmarkCheck")

            result.task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("explains that no baseline has been recorded yet") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)

            val result = fixture.buildAndFailWithoutExecution("kreateBenchmarkCheck")

            result.output shouldContain "No benchmark baseline has been recorded"
            result.output shouldContain "./gradlew :kreateBenchmarkBaseline"
        }

        test("fails when a benchmark disappeared from the run") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)
            fixture.buildWithoutExecution("kreateBenchmarkBaseline")

            fixture.write("build/reports/benchmarks/main/2026-08-19T11.00.00/benchmarks.json", "[]")
            val result = fixture.buildAndFailWithoutExecution("kreateBenchmarkCheck")

            result.output shouldContain "in the baseline but not in this run"
        }

        test("refuses a gate profile that cannot produce a readable report") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild(
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

        test("warns when the threshold can never be reached") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild(
                """
                enabled = true
                regression {
                    maxRegressionPercent = 150.0
                }
                """.trimIndent()
            )

            val result = fixture.build("tasks")

            result.output shouldContain "cannot drop by more than"
        }

        test("reuses the configuration cache for the gate") {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild()
            fixture.seedReport(score = 1000.0)
            fixture.buildWithoutExecution("kreateBenchmarkBaseline")
            fixture.buildWithoutExecution("kreateBenchmarkCheck")

            val result = fixture.buildWithoutExecution("kreateBenchmarkCheck")

            result.output shouldContain "Configuration cache entry reused"
        }

        test("runs a real benchmark end to end and compares it").config(tags = setOf(Slow)) {
            val fixture = newBuild()
            fixture.writeBenchmarkBuild(
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

            val result = fixture.build("kreateBenchmarkBaseline")

            result.task(":$MAIN_PROFILE_EXECUTION_TASK")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.baseline().readText() shouldContain "com.example.SampleBenchmark.value"

            val check = fixture.build("kreateBenchmarkCheck")
            check.task(":kreateBenchmarkCheck")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }
})

private fun KreateBuildFixture.writeBenchmarkBuild(
    benchmarkBlock: String = "enabled = true",
    isPluginApplied: Boolean = true
) {
    writeBuild(
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
        extraPlugins = listOfNotNull(BENCHMARK_PLUGIN_FROM_TESTKIT_CLASSPATH.takeIf { isPluginApplied })
    )
}

private fun KreateBuildFixture.seedReport(
    score: Double,
    timestamp: String = DEFAULT_REPORT_TIMESTAMP,
    error: Double = DEFAULT_SCORE_ERROR
) {
    write(
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

private fun KreateBuildFixture.baseline(): File = file("benchmarks/baseline.json")

private fun KreateBuildFixture.buildWithoutExecution(vararg tasks: String): BuildResult =
    build(*tasks, "-x", MAIN_PROFILE_EXECUTION_TASK)

private fun KreateBuildFixture.buildAndFailWithoutExecution(vararg tasks: String): BuildResult =
    buildAndFail(*tasks, "-x", MAIN_PROFILE_EXECUTION_TASK)
