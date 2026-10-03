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

package com.davils.kreate.benchmark.wiring

import com.davils.kreate.benchmark.BenchmarkExtension
import com.davils.kreate.benchmark.BenchmarkProfileExtension
import com.davils.kreate.benchmark.BenchmarkRegressionExtension
import com.davils.kreate.benchmark.BenchmarkTaskNames
import com.davils.kreate.benchmark.MAIN_BENCHMARK_PROFILE
import com.davils.kreate.benchmark.comparison.BenchmarkDirection
import com.davils.kreate.benchmark.task.BenchmarkBaseline
import com.davils.kreate.benchmark.task.BenchmarkCheck
import com.davils.kreate.benchmark.task.NormalizeBenchmarkReport
import com.davils.kreate.gradle.qualifiedTaskPath
import kotlinx.benchmark.gradle.BenchmarkConfiguration
import kotlinx.benchmark.gradle.BenchmarksExtension
import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

private const val MAX_THROUGHPUT_REGRESSION_PERCENT = 100.0
private const val JSON_REPORT_FORMAT = "json"
private const val EXECUTION_TASK_SUFFIX = "Benchmark"
private const val KREATE_BENCHMARK_REPORTS_PATH = "reports/kreate/benchmark"

internal fun Project.configureBenchmarks(extension: BenchmarkExtension) {
    val targetName = setUpBenchmarkSourceSet(extension)

    extensions.configure<BenchmarksExtension> {
        extension.profiles.forEach { profile ->
            configurations.maybeCreate(profile.name).applyProfile(profile)
        }
        registerProfileTargets(targetName, extension)
        registerBenchmarkTasks(extension, targetName, reportsDir)
    }
}

private fun BenchmarksExtension.registerProfileTargets(
    targetName: String,
    extension: BenchmarkExtension
) {
    targets.register(targetName) {
        if (this is JvmBenchmarkTarget) jmhVersion = extension.jmhVersion.get()
    }
}

private fun BenchmarkConfiguration.applyProfile(profile: BenchmarkProfileExtension) {
    warmups = profile.warmups.get()
    iterations = profile.iterations.get()
    iterationTime = profile.iterationTime.get()
    iterationTimeUnit = profile.iterationTimeUnit.get()
    mode = profile.mode.get()
    reportFormat = profile.reportFormat.get()
    profile.outputTimeUnit.orNull?.let { outputTimeUnit = it }

    profile.includes.get().forEach(::include)
    profile.excludes.get().forEach(::exclude)
    profile.params.get().forEach { (name, values) -> values.forEach { param(name, it) } }
    profile.advanced.get().forEach { (name, value) -> advanced(name, value) }
}

private fun Project.registerBenchmarkTasks(
    extension: BenchmarkExtension,
    targetName: String,
    reportsDir: String
) {
    val regression = extension.regression
    if (!regression.enabled.get()) return

    val profileName = regression.profile.get()
    verifyGateProfile(extension, profileName)
    warnOnUnreachableThreshold(extension, profileName, regression.maxRegressionPercent.get())

    val lazilyResolvedExecutionTaskName = targetName + profileName.executionTaskInfix() + EXECUTION_TASK_SUFFIX
    val normalizeTask = tasks.register<NormalizeBenchmarkReport>(BenchmarkTaskNames.REPORT) {
        dependsOn(lazilyResolvedExecutionTaskName)
        profile.set(profileName)
        timestampedReportsDirectory.set(layout.buildDirectory.dir("$reportsDir/$profileName"))
        normalizedDirectory.set(layout.buildDirectory.dir("$KREATE_BENCHMARK_REPORTS_PATH/$profileName"))
    }

    val recordBaselineTask = tasks.register<BenchmarkBaseline>(BenchmarkTaskNames.BASELINE) {
        normalizedReports.from(normalizeTask)
        profile.set(profileName)
        baselineFile.set(regression.baselineFile)
    }

    registerCheckTask(regression, profileName, normalizeTask, recordBaselineTask)
}

private fun Project.registerCheckTask(
    regression: BenchmarkRegressionExtension,
    profileName: String,
    normalizeTask: TaskProvider<NormalizeBenchmarkReport>,
    recordBaselineTask: TaskProvider<BenchmarkBaseline>
) {
    val projectPath = path
    val baselineTaskPath = qualifiedTaskPath(BenchmarkTaskNames.BASELINE)

    tasks.register<BenchmarkCheck>(BenchmarkTaskNames.CHECK) {
        mustRunAfter(recordBaselineTask)
        normalizedReports.from(normalizeTask)
        baselineFile.from(regression.baselineFile)
        profile.set(profileName)
        maxRegressionPercent.set(regression.maxRegressionPercent)
        thresholdOverrides.set(regression.thresholdOverrides)
        failOnMissingBenchmark.set(regression.failOnMissingBenchmark)
        requireSignificance.set(regression.requireSignificance)
        this.projectPath.set(projectPath)
        this.baselineTaskPath.set(baselineTaskPath)
        comparisonReportFile.set(layout.buildDirectory.file("$KREATE_BENCHMARK_REPORTS_PATH/comparison.md"))
    }
}

private fun verifyGateProfile(extension: BenchmarkExtension, profileName: String) {
    val profile = extension.profiles.findByName(profileName)
    if (profile == null) {
        requireImplicitMainProfile(profileName)
        return
    }

    val format = profile.reportFormat.get()
    val isJsonReport = format.equals(JSON_REPORT_FORMAT, ignoreCase = true)
    if (!isJsonReport) {
        throw GradleException(
            "The benchmark regression gate reads profile '$profileName', which is " +
                "configured to report '$format'. The gate can only read '$JSON_REPORT_FORMAT'; " +
                "passing a run it cannot parse would be worse than failing here."
        )
    }
}

private fun requireImplicitMainProfile(profileName: String) {
    if (profileName == MAIN_BENCHMARK_PROFILE) return

    throw GradleException(
        "The benchmark regression gate reads profile '$profileName', which is not " +
            "configured. Register it under `benchmark { profiles { register(\"" +
            "$profileName\") { } } }`, or point `regression { profile }` at one that exists."
    )
}

private fun Project.warnOnUnreachableThreshold(
    extension: BenchmarkExtension,
    profileName: String,
    threshold: Double
) {
    val mode = extension.profiles.findByName(profileName)?.mode?.get() ?: return
    val measuresThroughput = BenchmarkDirection.forMode(mode) == BenchmarkDirection.HIGHER_IS_BETTER
    val isUnreachable = measuresThroughput && threshold >= MAX_THROUGHPUT_REGRESSION_PERCENT
    if (!isUnreachable) return

    logger.warn(
        "Kreate's benchmark gate for project '$path' is set to $threshold%, but profile " +
            "'$profileName' measures '$mode', where a score cannot drop by more than " +
            "$MAX_THROUGHPUT_REGRESSION_PERCENT%. No regression can reach that threshold, so " +
            "the gate will never fail. Lower `regression { maxRegressionPercent }`, or set " +
            "`regression { enabled = false }` if switching it off is what you meant."
    )
}

private fun String.executionTaskInfix(): String {
    if (this == MAIN_BENCHMARK_PROFILE) return ""
    return replaceFirstChar { it.titlecase() }
}
