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
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * A named verification rule, holding one or more bounds evaluated against the same grouping.
 *
 * Rules are named because the name is what a developer sees when the build fails. `Rule
 * 'Minimum line coverage' violated` sends someone to the right place; the unnamed alternative
 * reports an index.
 *
 * @param name The rule name, supplied by the container.
 * @param factory The object factory used for creating properties and bounds.
 * @since 2.2.0
 */
public abstract class CoverageRuleSpec @Inject constructor(
    private val name: String,
    private val factory: ObjectFactory
) : Named {
    /**
     * Whether this rule is skipped.
     *
     * Defaults to `false`. Preferable to deleting a rule while a codebase is being brought back
     * up to it, because a disabled rule still documents the intent.
     *
     * @since 2.2.0
     */
    public val disabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The entity this rule's bounds are evaluated against.
     *
     * Unset by default, which inherits [CoverageVerifyExtension.groupBy].
     *
     * @since 2.2.0
     */
    public val groupBy: Property<Grouping> = factory.property(Grouping::class.java)

    /**
     * The bounds this rule checks.
     *
     * Populated through [bound], [minBound] and [maxBound] rather than assigned directly.
     *
     * @since 2.2.0
     */
    public val bounds: ListProperty<CoverageBoundSpec> =
        factory.listProperty(CoverageBoundSpec::class.java).convention(emptyList())

    /**
     * Returns the name of this rule.
     *
     * @return The rule name.
     * @since 2.2.0
     */
    override fun getName(): String = name

    /**
     * Adds a bound configured by the given action.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun bound(action: Action<CoverageBoundSpec>) {
        val spec = newBound()
        action.execute(spec)
        bounds.add(spec)
    }

    private fun newBound(): CoverageBoundSpec = factory.newInstance(CoverageBoundSpec::class.java)

    /**
     * Adds a bound with only a lower limit.
     *
     * @param value The lowest acceptable value.
     * @param unit The unit the bound is measured in. Defaults to [CoverageUnit.LINE].
     * @param aggregation How measurements are aggregated. Defaults to
     * [Aggregation.COVERED_PERCENTAGE].
     * @since 2.2.0
     */
    @JvmOverloads
    public fun minBound(
        value: Int,
        unit: CoverageUnit = CoverageUnit.LINE,
        aggregation: Aggregation = Aggregation.COVERED_PERCENTAGE
    ) {
        val spec = newBound()
        spec.min.set(value)
        spec.unit.set(unit)
        spec.aggregation.set(aggregation)
        bounds.add(spec)
    }

    /**
     * Adds a bound with only an upper limit.
     *
     * @param value The highest acceptable value.
     * @param unit The unit the bound is measured in. Defaults to [CoverageUnit.LINE].
     * @param aggregation How measurements are aggregated. Defaults to
     * [Aggregation.COVERED_PERCENTAGE].
     * @since 2.2.0
     */
    @JvmOverloads
    public fun maxBound(
        value: Int,
        unit: CoverageUnit = CoverageUnit.LINE,
        aggregation: Aggregation = Aggregation.COVERED_PERCENTAGE
    ) {
        val spec = newBound()
        spec.max.set(value)
        spec.unit.set(unit)
        spec.aggregation.set(aggregation)
        bounds.add(spec)
    }
}
