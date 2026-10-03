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

package com.davils.kreate.coverage.wiring

import com.davils.kreate.coverage.CoverageBoundSpec
import com.davils.kreate.coverage.CoverageRuleSpec
import com.davils.kreate.coverage.CoverageVerifyExtension
import com.davils.kreate.coverage.Grouping
import kotlinx.kover.gradle.plugin.dsl.KoverVerifyRule
import kotlinx.kover.gradle.plugin.dsl.KoverVerifyTaskConfig
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import kotlinx.kover.gradle.plugin.dsl.AggregationType as KoverAggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit as KoverCoverageUnit

internal fun KoverVerifyTaskConfig.configureVerification(extension: CoverageVerifyExtension) {
    onCheck.set(extension.runOnCheck)
    warningInsteadOfFailure.set(extension.warningInsteadOfFailure)

    val defaultGrouping = extension.groupBy.get()
    registerShorthand("Minimum line coverage", extension.minLineCoverage, KoverCoverageUnit.LINE, defaultGrouping)
    registerShorthand(
        "Minimum branch coverage",
        extension.minBranchCoverage,
        KoverCoverageUnit.BRANCH,
        defaultGrouping
    )
    registerShorthand(
        "Minimum instruction coverage",
        extension.minInstructionCoverage,
        KoverCoverageUnit.INSTRUCTION,
        defaultGrouping
    )

    extension.rules.forEach { spec -> registerRule(spec, defaultGrouping) }
}

private fun KoverVerifyTaskConfig.registerShorthand(
    name: String,
    minimum: Property<Int>,
    unit: KoverCoverageUnit,
    defaultGrouping: Grouping
) {
    if (!minimum.isPresent) return

    val minimumPercent = minimum.get()
    rule(name) {
        groupBy.set(defaultGrouping.toKover())
        minBound(minimumPercent, unit, KoverAggregationType.COVERED_PERCENTAGE)
    }
}

private fun KoverVerifyTaskConfig.registerRule(spec: CoverageRuleSpec, defaultGrouping: Grouping) {
    val specBounds = spec.bounds.get()
    requireBounds(spec.name, specBounds)

    rule(spec.name) {
        disabled.set(spec.disabled)
        groupBy.set(spec.groupBy.orElse(defaultGrouping).map { it.toKover() })
        specBounds.forEach { boundSpec -> addBound(boundSpec) }
    }
}

private fun requireBounds(ruleName: String, specBounds: List<CoverageBoundSpec>) {
    if (specBounds.isEmpty()) {
        throw GradleException(
            "Coverage rule '$ruleName' declares no bounds. A rule without a bound checks " +
                "nothing and always passes; give it a `bound { }`, `minBound(...)` or " +
                "`maxBound(...)`, or remove it."
        )
    }

    val hasUnlimitedBound = specBounds.any { !it.min.isPresent && !it.max.isPresent }
    if (hasUnlimitedBound) {
        throw GradleException(
            "A bound of coverage rule '$ruleName' sets neither `min` nor `max`. " +
                "Such a bound measures something and demands nothing."
        )
    }
}

private fun KoverVerifyRule.addBound(boundSpec: CoverageBoundSpec) {
    bound {
        minValue.set(boundSpec.min)
        maxValue.set(boundSpec.max)
        coverageUnits.set(boundSpec.unit.get().toKover())
        aggregationForGroup.set(boundSpec.aggregation.get().toKover())
    }
}
