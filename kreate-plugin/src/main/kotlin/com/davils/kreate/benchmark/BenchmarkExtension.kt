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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring JMH based benchmarks through kotlinx-benchmark.
 *
 * When enabled, Kreate builds the benchmark source set, wires the runtime dependency and
 * the `allopen` compiler plugin, configures the measurement profiles, and registers a
 * regression gate over the resulting report.
 *
 * Kreate does not apply the kotlinx-benchmark plugin. Apply it yourself so its version
 * stays under your control:
 *
 * ```kotlin
 * plugins {
 *     id("org.jetbrains.kotlinx.benchmark") version "<version>"
 * }
 * ```
 *
 * @since 2.2.0
 */
public abstract class BenchmarkExtension @Inject constructor(factory: ObjectFactory, project: Project) {
    /**
     * Whether benchmark support is enabled for this project.
     *
     * @since 2.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The name of the source set holding the benchmarks.
     *
     * Defaults to `benchmarks`. Benchmarks live apart from `main` so that JMH's generated
     * code and the `allopen` transformation never reach the artifact you publish.
     *
     * @since 2.2.0
     */
    public val sourceSetName: Property<String> =
        factory.property(String::class.java).convention("benchmarks")

    /**
     * Whether Kreate creates and wires the benchmark source set.
     *
     * Defaults to `true`. Set it to `false` to keep a hand-written setup and let Kreate
     * configure only the profiles and the regression gate.
     *
     * @since 2.2.0
     */
    public val createSourceSet: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    /**
     * The version of `kotlinx-benchmark-runtime` added to the benchmark source set.
     *
     * Keep it in step with the version of the kotlinx-benchmark plugin you applied; the
     * runtime and the plugin are released together.
     *
     * @since 2.2.0
     */
    public val runtimeVersion: Property<String> =
        factory.property(String::class.java).convention(DEFAULT_RUNTIME_VERSION)

    /**
     * The JMH version used for JVM targets.
     *
     * @since 2.2.0
     */
    public val jmhVersion: Property<String> =
        factory.property(String::class.java).convention(DEFAULT_JMH_VERSION)

    /**
     * Whether Kreate applies the `allopen` compiler plugin for the JMH state annotation.
     *
     * Defaults to `true`. JMH subclasses every `@State` class, so a benchmark class that is
     * final fails at generation time with a message that never mentions `allopen`. Unlike
     * the benchmark plugin itself there is nothing to decide here, which is why Kreate
     * applies this one rather than asking.
     *
     * @since 2.2.0
     */
    public val applyAllOpen: Property<Boolean> =
        factory.property(Boolean::class.java).convention(true)

    /**
     * The measurement profiles.
     *
     * `main` always exists. Registering another name creates a second set of tasks and a
     * second report directory, which is how a short profile for continuous integration
     * lives beside a long one.
     *
     * @since 2.2.0
     */
    public val profiles: NamedDomainObjectContainer<BenchmarkProfileExtension> =
        project.objects.domainObjectContainer(BenchmarkProfileExtension::class.java).apply {
            register(MAIN_BENCHMARK_PROFILE)
        }

    /**
     * Configuration for the regression gate.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val regression: BenchmarkRegressionExtension

    /**
     * Configures the measurement profiles.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun profiles(action: Action<NamedDomainObjectContainer<BenchmarkProfileExtension>>) {
        action.execute(profiles)
    }

    /**
     * Configures the [BenchmarkRegressionExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun regression(action: Action<BenchmarkRegressionExtension>) {
        action.execute(regression)
    }

    private companion object {
        private const val DEFAULT_RUNTIME_VERSION = "0.4.17"
        private const val DEFAULT_JMH_VERSION = "1.37"
    }
}
