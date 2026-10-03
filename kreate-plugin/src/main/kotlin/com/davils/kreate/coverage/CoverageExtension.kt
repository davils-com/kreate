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

package com.davils.kreate.coverage

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring code coverage through Kover.
 *
 * Detekt says whether the code looks right and Trivy says whether it is safe to ship, but neither
 * says whether the code is ever executed. Coverage is the measurement that closes that gap: a
 * suite can be entirely green while half the codebase is never entered.
 *
 * Kreate configures Kover but does not apply it — the `org.jetbrains.kotlinx.kover` plugin has to
 * be applied by the consumer, who then owns its version. Enabling this integration without that
 * plugin fails with an explanatory message rather than silently doing nothing.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether the coverage integration is enabled for this project.
     *
     * Defaults to `false`.
     *
     * @since 2.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether coverage is measured with JaCoCo instead of Kover's own engine.
     *
     * Defaults to `false`. JaCoCo exists here for builds that have to interoperate with tooling
     * expecting it; it is not feature-equivalent, and filtering by annotation in particular does
     * not work with it.
     *
     * @since 2.2.0
     */
    public val useJacoco: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The JaCoCo version used when [useJacoco] is enabled.
     *
     * Unset by default, which leaves Kover's bundled version in place.
     *
     * @since 2.2.0
     */
    public val jacocoVersion: Property<String> = factory.property(String::class.java)

    /**
     * Configuration for which source sets are measured.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val sources: CoverageSourcesExtension

    /**
     * Configuration for which classes are instrumented.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val instrumentation: CoverageInstrumentationExtension

    /**
     * Configuration for which classes appear in the reports.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val filters: CoverageFilterExtension

    /**
     * Configuration for the coverage reports.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val reports: CoverageReportExtension

    /**
     * Configuration for the coverage thresholds.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val verify: CoverageVerifyExtension

    /**
     * Configuration for merging the coverage of other projects into this one's reports.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val aggregate: CoverageAggregateExtension

    /**
     * Configures the [CoverageSourcesExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun sources(action: Action<CoverageSourcesExtension>) {
        action.execute(sources)
    }

    /**
     * Configures the [CoverageInstrumentationExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun instrumentation(action: Action<CoverageInstrumentationExtension>) {
        action.execute(instrumentation)
    }

    /**
     * Configures the [CoverageFilterExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun filters(action: Action<CoverageFilterExtension>) {
        action.execute(filters)
    }

    /**
     * Configures the [CoverageReportExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun reports(action: Action<CoverageReportExtension>) {
        action.execute(reports)
    }

    /**
     * Configures the [CoverageVerifyExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun verify(action: Action<CoverageVerifyExtension>) {
        action.execute(verify)
    }

    /**
     * Configures the [CoverageAggregateExtension] using the provided action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun aggregate(action: Action<CoverageAggregateExtension>) {
        action.execute(aggregate)
    }
}
