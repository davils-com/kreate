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

package com.davils.kreate.module.project.tests

import com.davils.kreate.module.project.tests.logging.TestsLoggingExtension
import com.davils.kreate.module.project.tests.report.TestsReportExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring testing settings in Kreate.
 *
 * This extension provides properties for test execution control, such as
 * parallelism, timeouts, and failure handling, as well as nested
 * configurations for logging and reporting.
 *
 * @param factory The object factory used for creating properties.
 * @since 1.0.0
 */
public abstract class TestsExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 1.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether testing is enabled.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The maximum number of parallel forks for test execution.
     * Defaults to half of the available processors.
     * @since 1.0.0
     */
    public val maxParallelForks: Property<Int> = factory.property(
        Int::class.java
    ).convention(Runtime.getRuntime().availableProcessors() / 2)

    /**
     * The timeout for test execution in minutes.
     * Defaults to 10 minutes.
     * @since 1.0.0
     */
    public val timeoutMinutes: Property<Long> = factory.property(Long::class.java).convention(10L)

    /**
     * Whether to ignore test failures and continue the build.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val ignoreFailures: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    /**
     * Whether tests should always run, even if they are up-to-date.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val alwaysRunTests: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    /**
     * Whether to fail the build if no tests are discovered.
     * Defaults to `false`.
     * @since 1.0.0
     */
    public val failOnNoDiscoveredTests: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    /**
     * Configuration for test logging.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val logging: TestsLoggingExtension

    /**
     * Configuration for test reporting.
     * @since 1.0.0
     */
    @get:Nested
    public abstract val report: TestsReportExtension

    /**
     * Configures the [TestsLoggingExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun logging(action: Action<TestsLoggingExtension>) {
        action.execute(logging)
    }

    /**
     * Configures the [TestsReportExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 1.0.0
     */
    public fun report(action: Action<TestsReportExtension>) {
        action.execute(report)
    }
}
