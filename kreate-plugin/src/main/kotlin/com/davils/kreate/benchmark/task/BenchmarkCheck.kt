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

package com.davils.kreate.benchmark.task

import com.davils.kreate.benchmark.comparison.BenchmarkComparator
import com.davils.kreate.benchmark.comparison.BenchmarkDelta
import com.davils.kreate.benchmark.comparison.ComparisonRenderer
import com.davils.kreate.benchmark.comparison.RegressionSettings
import com.davils.kreate.benchmark.report.BenchmarkReport
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
internal abstract class BenchmarkCheck : KreateTask(
    "Compares the benchmark results against the committed baseline.",
    KreateTaskGroup.BENCHMARK
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val normalizedReports: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val baselineFile: ConfigurableFileCollection

    @get:Input
    public abstract val profile: Property<String>

    @get:Input
    public abstract val maxRegressionPercent: Property<Double>

    @get:Input
    public abstract val thresholdOverrides: MapProperty<String, Double>

    @get:Input
    public abstract val failOnMissingBenchmark: Property<Boolean>

    @get:Input
    public abstract val requireSignificance: Property<Boolean>

    @get:Input
    public abstract val projectPath: Property<String>

    @get:Input
    public abstract val baselineTaskPath: Property<String>

    @get:OutputFile
    public abstract val comparisonReportFile: RegularFileProperty

    @TaskAction
    public fun execute() {
        val currentFile = normalizedReports.firstNormalizedReport(profile.get())
        val baseline = baselineFile.files.singleOrNull()?.takeIf { it.isFile }
            ?: missingBaselineFailure()

        val settings = RegressionSettings(
            maxRegressionPercent = maxRegressionPercent.get(),
            thresholdOverrides = thresholdOverrides.get(),
            failOnMissingBenchmark = failOnMissingBenchmark.get(),
            requireSignificance = requireSignificance.get()
        )

        val deltas = BenchmarkComparator.compare(
            baseline = BenchmarkReport.parse(baseline.readText()),
            current = BenchmarkReport.parse(currentFile.readText()),
            settings = settings
        )

        writeComparison(deltas)

        val failures = BenchmarkComparator.failures(deltas, settings)
        if (failures.isEmpty()) {
            logger.lifecycle("Compared ${deltas.size} benchmark(s) against the baseline; no regression.")
            return
        }

        throw GradleException(regressionMessage(failures))
    }

    private fun regressionMessage(failures: List<BenchmarkDelta>): String =
        listOf(
            "Benchmark regression in project '${projectPath.get()}' " +
                "(profile '${profile.get()}'):",
            "",
            ComparisonRenderer.renderFailures(failures),
            "",
            "If the change is expected, record the new baseline and commit the result:",
            "",
            "    ./gradlew ${baselineTaskPath.get()}",
            "",
            "Full comparison: ${comparisonReportFile.get().asFile.path}"
        ).joinToString("\n")

    private fun missingBaselineFailure(): Nothing = throw GradleException(
        listOf(
            "No benchmark baseline has been recorded for project '${projectPath.get()}' yet.",
            "",
            "Create it and commit the result:",
            "",
            "    ./gradlew ${baselineTaskPath.get()}"
        ).joinToString("\n")
    )

    private fun writeComparison(deltas: List<BenchmarkDelta>) {
        val target: File = comparisonReportFile.get().asFile
        target.parentFile.mkdirs()
        target.writeText(ComparisonRenderer.renderMarkdown(deltas, profile.get()))
    }
}
