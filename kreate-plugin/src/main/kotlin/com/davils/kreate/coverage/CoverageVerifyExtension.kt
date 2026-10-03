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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The coverage thresholds the build is verified against.
 *
 * None of the three shorthand bounds has a default. A number picked before the first measurement
 * either breaks the build on the day the feature is switched on, or sits so far below the real
 * figure that it can never fail — and a gate that cannot fail is worse than no gate, because it
 * reads like protection. Measure first with `koverLog`, then set the bound to what you actually
 * have, and raise it from there. A bound that is left unset registers no rule at all rather than
 * a rule demanding zero coverage.
 *
 * [minLineCoverage], [minBranchCoverage] and [minInstructionCoverage] cover the common case.
 * Anything beyond it — several bounds in one rule, absolute counts, per-class grouping for one
 * rule but not another — goes through [rules].
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageVerifyExtension @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether verification runs as part of the `check` task.
     *
     * Defaults to `true`. A threshold that has to be asked for by name is a threshold nobody
     * finds out about until someone remembers to look.
     *
     * @since 2.2.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether a violated bound logs a warning instead of failing the build.
     *
     * Defaults to `false`. Useful for the transitional period while a codebase is being brought
     * up to a threshold, and dangerous as a permanent setting for the reason described above.
     *
     * @since 2.2.0
     */
    public val warningInsteadOfFailure: Property<Boolean> =
        factory.property(Boolean::class.java).convention(false)

    /**
     * The entity the bounds are evaluated against, unless a rule overrides it.
     *
     * Defaults to [Grouping.APPLICATION], which checks each bound once against the whole
     * codebase. [Grouping.CLASS] checks every class separately and is considerably stricter.
     *
     * @since 2.2.0
     */
    public val groupBy: Property<Grouping> =
        factory.property(Grouping::class.java).convention(Grouping.APPLICATION)

    /**
     * The minimum percentage of covered lines, between 0 and 100.
     *
     * Unset by default, which registers no line coverage rule.
     *
     * @since 2.2.0
     */
    public val minLineCoverage: Property<Int> = factory.property(Int::class.java)

    /**
     * The minimum percentage of covered branches, between 0 and 100.
     *
     * Unset by default, which registers no branch coverage rule. Worth setting alongside
     * [minLineCoverage]: a suite can cover every line while only ever taking one side of each
     * condition.
     *
     * @since 2.2.0
     */
    public val minBranchCoverage: Property<Int> = factory.property(Int::class.java)

    /**
     * The minimum percentage of covered bytecode instructions, between 0 and 100.
     *
     * Unset by default, which registers no instruction coverage rule.
     *
     * @since 2.2.0
     */
    public val minInstructionCoverage: Property<Int> = factory.property(Int::class.java)

    /**
     * Named verification rules, for thresholds the shorthand properties cannot express.
     *
     * Rules registered here are added to those the shorthands produce, not used instead of them.
     *
     * @since 2.2.0
     */
    public val rules: NamedDomainObjectContainer<CoverageRuleSpec> =
        factory.domainObjectContainer(CoverageRuleSpec::class.java)

    /**
     * Configures the named verification rules.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun rules(action: Action<NamedDomainObjectContainer<CoverageRuleSpec>>) {
        action.execute(rules)
    }
}
