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

package com.davils.kreate.benchmark

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the benchmark regression gate.
 *
 * The gate compares a benchmark run against a baseline committed to the repository, the
 * same way `kreateApiCheck` compares the compiled classes against a committed API dump. A
 * benchmark nobody holds against an earlier measurement is a number in a build log; this
 * is what turns it into something a review can act on.
 *
 * @since 2.2.0
 */
public abstract class BenchmarkRegressionExtension @Inject constructor(factory: ObjectFactory, project: Project) {
    /**
     * Whether the regression tasks are registered.
     *
     * Defaults to `true`, so enabling benchmarks at all also gives you the gate. Set it to
     * `false` to run benchmarks without comparing them to anything.
     *
     * @since 2.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The profile whose report the gate reads.
     *
     * @since 2.2.0
     */
    public val profile: Property<String> = factory.property(String::class.java).convention(MAIN_BENCHMARK_PROFILE)

    /**
     * The committed baseline the run is compared against.
     *
     * Defaults to `benchmarks/baseline.json` below the project directory.
     *
     * @since 2.2.0
     */
    public val baselineFile: RegularFileProperty = factory.fileProperty().convention(
        project.layout.projectDirectory.file("benchmarks/baseline.json")
    )

    /**
     * How far a benchmark may move in the worse direction before the build fails, in per cent.
     *
     * Defaults to `10.0`. On a shared continuous integration runner even a correct number
     * here will not catch small regressions — see [requireSignificance].
     *
     * @since 2.2.0
     */
    public val maxRegressionPercent: Property<Double> =
        factory.property(Double::class.java).convention(DEFAULT_MAX_REGRESSION_PERCENT)

    /**
     * Per-benchmark thresholds, keyed by fully qualified benchmark name.
     *
     * A benchmark listed here uses its own limit instead of [maxRegressionPercent], which
     * is how a known-noisy measurement stays in the suite without loosening the gate for
     * everything else.
     *
     * @since 2.2.0
     */
    public val thresholdOverrides: MapProperty<String, Double> =
        factory.mapProperty(String::class.java, Double::class.java)

    /**
     * Whether a benchmark present in the baseline but absent from the run fails the build.
     *
     * Defaults to `true`. Deleting a benchmark is the simplest way to make a regression
     * disappear, so its absence has to be a decision someone records rather than something
     * the gate ignores.
     *
     * @since 2.2.0
     */
    public val failOnMissingBenchmark: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    /**
     * Whether a regression must also exceed the combined measurement error to count.
     *
     * Defaults to `true`. Without this test the gate fires on ordinary run-to-run variance,
     * and a gate that cries wolf is switched off within a week. When a run reports no error
     * margin — a single iteration does not produce one — the threshold decides on its own.
     *
     * @since 2.2.0
     */
    public val requireSignificance: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    private companion object {
        private const val DEFAULT_MAX_REGRESSION_PERCENT = 10.0
    }
}
