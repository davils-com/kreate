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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One threshold within a verification rule.
 *
 * A bound pairs a measurement — a [unit] aggregated one particular way — with a limit on it.
 * Setting neither [min] nor [max] leaves a bound that measures something and demands nothing,
 * which is why the integration rejects it rather than registering it.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageBoundSpec @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * The lowest acceptable value.
     *
     * Unset by default. Interpreted as a percentage or an absolute count depending on
     * [aggregation].
     *
     * @since 2.2.0
     */
    public val min: Property<Int> = factory.property(Int::class.java)

    /**
     * The highest acceptable value.
     *
     * Unset by default. Useful with [Aggregation.MISSED_COUNT], where it caps how much untested
     * code may exist at all — a limit a percentage cannot express, because it moves as the
     * codebase grows.
     *
     * @since 2.2.0
     */
    public val max: Property<Int> = factory.property(Int::class.java)

    /**
     * The unit the bound is measured in.
     *
     * Defaults to [CoverageUnit.LINE].
     *
     * @since 2.2.0
     */
    public val unit: Property<CoverageUnit> =
        factory.property(CoverageUnit::class.java).convention(CoverageUnit.LINE)

    /**
     * How the measurements of a group are aggregated into the value this bound checks.
     *
     * Defaults to [Aggregation.COVERED_PERCENTAGE].
     *
     * @since 2.2.0
     */
    public val aggregation: Property<Aggregation> =
        factory.property(Aggregation::class.java).convention(Aggregation.COVERED_PERCENTAGE)
}
