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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject

/**
 * A named set of measurement settings, corresponding to one kotlinx-benchmark configuration.
 *
 * Every profile produces its own set of Gradle tasks and its own report directory, so a
 * project can keep a short profile for continuous integration next to a long one that is
 * run deliberately.
 *
 * Only the settings that affect reproducibility and the regression gate are exposed here.
 * Anything else stays available through the `benchmark { }` block kotlinx-benchmark
 * installs; Kreate writes only the values listed below and leaves the rest untouched.
 *
 * @since 2.2.0
 */
public abstract class BenchmarkProfileExtension @Inject constructor(
    /**
     * The name of this profile, used for its task names and report directory.
     * @since 2.2.0
     */
    public val name: String,
    factory: ObjectFactory
) {
    /**
     * The number of warmup iterations run before measurement starts.
     *
     * Defaults to `5`. Warmups exist so that the JIT has settled before anything is
     * recorded; a benchmark measured without them reports the interpreter.
     *
     * @since 2.2.0
     */
    public val warmups: Property<Int> = factory.property(Int::class.java).convention(DEFAULT_WARMUPS)

    /**
     * The number of measurement iterations.
     *
     * Defaults to `5`. More iterations narrow the confidence interval, which is what the
     * regression gate uses to tell a real change from noise.
     *
     * @since 2.2.0
     */
    public val iterations: Property<Int> = factory.property(Int::class.java).convention(DEFAULT_ITERATIONS)

    /**
     * The duration of a single iteration, in [iterationTimeUnit].
     *
     * @since 2.2.0
     */
    public val iterationTime: Property<Long> =
        factory.property(Long::class.java).convention(DEFAULT_ITERATION_TIME)

    /**
     * The unit of [iterationTime].
     *
     * One of `ns`, `us`, `ms`, `s`, `m`, or the corresponding long names.
     *
     * @since 2.2.0
     */
    public val iterationTimeUnit: Property<String> =
        factory.property(String::class.java).convention("s")

    /**
     * The measurement mode.
     *
     * `thrpt` measures operations per unit of time and is better when higher; `avgt`
     * measures time per operation and is better when lower. The regression gate reads this
     * to know which direction counts as a regression.
     *
     * @since 2.2.0
     */
    public val mode: Property<String> = factory.property(String::class.java).convention("thrpt")

    /**
     * The unit scores are reported in.
     *
     * Left unset by default so that kotlinx-benchmark picks the unit that suits [mode].
     * Changing it invalidates an existing baseline, because the gate refuses to compare
     * scores recorded in different units.
     *
     * @since 2.2.0
     */
    public val outputTimeUnit: Property<String> = factory.property(String::class.java)

    /**
     * The report format.
     *
     * Defaults to `json`, which is the only format the regression gate can read. The gate
     * fails with a configuration error rather than passing silently when its profile uses
     * `csv`, `scsv` or `text`.
     *
     * @since 2.2.0
     */
    public val reportFormat: Property<String> = factory.property(String::class.java).convention("json")

    /**
     * Regular expressions selecting the benchmarks this profile runs.
     *
     * Empty means every benchmark.
     *
     * @since 2.2.0
     */
    public val includes: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * Regular expressions excluding benchmarks from this profile.
     *
     * @since 2.2.0
     */
    public val excludes: ListProperty<String> = factory.listProperty(String::class.java)

    /**
     * Values substituted into `@Param` properties, keyed by property name.
     *
     * @since 2.2.0
     */
    public val params: MapProperty<String, List<String>> = factory.mapProperty()

    /**
     * Platform specific settings passed through to kotlinx-benchmark's `advanced()`.
     *
     * Defaults to `jvmForks = 1`. A fixed fork count is the difference between a
     * measurement and a snapshot of whatever state the JIT happened to be in; leaving it to
     * `definedByJmh` makes results depend on how the build was invoked.
     *
     * @since 2.2.0
     */
    public val advanced: MapProperty<String, String> =
        factory.mapProperty(String::class.java, String::class.java)
            .convention(mapOf("jvmForks" to "1"))

    /**
     * Adds a regular expression to [includes].
     *
     * @param pattern The pattern matching fully qualified benchmark names.
     * @since 2.2.0
     */
    public fun include(pattern: String) {
        includes.add(pattern)
    }

    /**
     * Adds a regular expression to [excludes].
     *
     * @param pattern The pattern matching fully qualified benchmark names.
     * @since 2.2.0
     */
    public fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    /**
     * Declares the values a `@Param` property is run with.
     *
     * @param name The name of the annotated property.
     * @param values The values to substitute.
     * @since 2.2.0
     */
    public fun param(name: String, vararg values: String) {
        params.put(name, values.toList())
    }

    /**
     * Sets a platform specific option.
     *
     * @param name The option name, for example `jvmForks` or `nativeFork`.
     * @param value The option value.
     * @since 2.2.0
     */
    public fun advanced(name: String, value: String) {
        advanced.put(name, value)
    }

    private companion object {
        private const val DEFAULT_WARMUPS = 5
        private const val DEFAULT_ITERATIONS = 5
        private const val DEFAULT_ITERATION_TIME = 1L
    }
}
