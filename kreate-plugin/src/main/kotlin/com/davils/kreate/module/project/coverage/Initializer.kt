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

package com.davils.kreate.module.project.coverage

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.coverage.extension.CoverageAggregateExtension
import com.davils.kreate.module.project.coverage.extension.CoverageExtension
import com.davils.kreate.module.project.coverage.extension.CoverageFilterExtension
import com.davils.kreate.module.project.coverage.extension.CoverageFilterSpec
import com.davils.kreate.module.project.coverage.extension.CoverageReportExtension
import com.davils.kreate.module.project.coverage.extension.CoverageRuleSpec
import com.davils.kreate.module.project.coverage.extension.CoverageVerifyExtension
import com.davils.kreate.module.project.tests.suite.enabledSuites
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter
import kotlinx.kover.gradle.plugin.dsl.KoverReportSetConfig
import kotlinx.kover.gradle.plugin.dsl.KoverVerifyTaskConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import kotlinx.kover.gradle.plugin.dsl.AggregationType as KoverAggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit as KoverCoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType as KoverGroupingEntityType

/**
 * The id of the Kover Gradle plugin the integration configures.
 *
 * @since 2.2.0
 */
internal const val KOVER_PLUGIN_ID: String = "org.jetbrains.kotlinx.kover"

/**
 * The name of the Kover configuration that aggregated projects are added to.
 *
 * @since 2.2.0
 */
private const val KOVER_CONFIGURATION: String = "kover"

/**
 * Initializes the code coverage integration for the project.
 *
 * Configures Kover's project settings, reports and verification rules from the Kreate
 * configuration, if coverage is enabled.
 *
 * The Kover plugin itself is applied earlier, by `applyFeaturePlugins`.
 *
 * @param extension The main Kreate extension.
 * @since 2.2.0
 */
internal fun Project.initializeCoverage(extension: KreateExtension) {
    val coverageExtension = extension.project.coverage
    if (!coverageExtension.enabled.get()) {
        return
    }

    extensions.configure<KoverProjectExtension> {
        configureEngine(coverageExtension)
        configureCurrentProject(coverageExtension, suiteSourceSetsToExclude(extension))

        reports {
            total {
                configureFilters(coverageExtension.filters)
                configureReports(coverageExtension.reports)
                verify { configureVerification(coverageExtension.verify) }
            }
        }
    }

    configureAggregation(coverageExtension.aggregate)
}

/**
 * The compilations of the test suites, which must not be measured as production code.
 *
 * Kover recognises a test compilation by the name `test` and nothing else, so a suite called
 * `unitTest` lands in the denominator as if it were code the project ships. The build stays
 * green and the number quietly describes the wrong thing, which is the worst failure mode a
 * coverage report has.
 *
 * The name to exclude is the compilation's, which is the suite's source set name on both
 * Kotlin/JVM and multiplatform projects - not the per-target source set names derived from it.
 *
 * Returns nothing when the project has listed the source sets to measure explicitly: that list
 * already decides what counts, and adding exclusions to it would only be a second opinion.
 *
 * @param extension The main Kreate extension.
 * @return The suite compilation names to exclude from the measurement.
 * @since 3.0.0
 */
private fun suiteSourceSetsToExclude(extension: KreateExtension): Set<String> {
    val tests = extension.project.tests
    val explicitlyIncluded = extension.project.coverage.sources.includedSourceSets.get().isNotEmpty()
    val applicable = tests.enabled.get() && tests.excludeSuitesFromCoverage.get() && !explicitlyIncluded
    if (!applicable) return emptySet()

    return tests.enabledSuites().mapTo(mutableSetOf()) { it.sourceSetName.get() }
}

/**
 * Selects the coverage engine.
 *
 * @param extension The Kreate coverage configuration.
 * @since 2.2.0
 */
private fun KoverProjectExtension.configureEngine(extension: CoverageExtension) {
    useJacoco.set(extension.useJacoco)
    if (extension.jacocoVersion.isPresent) {
        jacocoVersion.set(extension.jacocoVersion)
    }
}

/**
 * Configures which source sets are measured and which classes are instrumented.
 *
 * @param extension The Kreate coverage configuration.
 * @param suiteSourceSets The test suite source sets that must not be measured.
 * @since 2.2.0
 */
private fun KoverProjectExtension.configureCurrentProject(
    extension: CoverageExtension,
    suiteSourceSets: Set<String>
) {
    currentProject {
        sources {
            excludeJava.set(extension.sources.excludeJava)
            includedSourceSets.set(extension.sources.includedSourceSets)
            excludedSourceSets.set(extension.sources.excludedSourceSets.map { it + suiteSourceSets })
        }

        instrumentation {
            disabledForAll.set(extension.instrumentation.disabledForAll)
            disabledForTestTasks.set(extension.instrumentation.disabledForTestTasks)
            includedClasses.set(extension.instrumentation.includedClasses)
            excludedClasses.set(extension.instrumentation.excludedClasses)
        }
    }
}

/**
 * Applies the report filters to a report set.
 *
 * @param extension The Kreate filter configuration.
 * @since 2.2.0
 */
private fun KoverReportSetConfig.configureFilters(extension: CoverageFilterExtension) {
    filters {
        excludes { applyCriteria(extension.excludes) }
        includes { applyCriteria(extension.includes) }
    }
}

/**
 * Copies one side of a filter onto Kover's filter model.
 *
 * @param spec The Kreate filter criteria.
 * @since 2.2.0
 */
private fun KoverReportFilter.applyCriteria(spec: CoverageFilterSpec) {
    classes.set(spec.classes)
    annotatedBy.set(spec.annotatedBy)
    inheritedFrom.set(spec.inheritedFrom)

    // Kover exposes no `packages` property — package patterns are folded into the class
    // patterns by this call, so it has to stay a call rather than an assignment. The value is
    // read eagerly, which is safe: the integration runs in `afterEvaluate`.
    packages(spec.packages.get())
}

/**
 * Configures the individual report formats of a report set.
 *
 * @param extension The Kreate report configuration.
 * @since 2.2.0
 */
private fun KoverReportSetConfig.configureReports(extension: CoverageReportExtension) {
    xml {
        onCheck.set(extension.xml.onCheck)
        xmlFile.set(extension.xml.file)
        if (extension.xml.title.isPresent) {
            title.set(extension.xml.title)
        }
    }

    html {
        onCheck.set(extension.html.onCheck)
        htmlDir.set(extension.html.directory)
        if (extension.html.title.isPresent) {
            title.set(extension.html.title)
        }
        if (extension.html.charset.isPresent) {
            charset.set(extension.html.charset)
        }
    }

    log {
        onCheck.set(extension.log.onCheck)
        format.set(extension.log.format)
        groupBy.set(extension.log.groupBy.get().toKover())
        coverageUnits.set(extension.log.coverageUnit.get().toKover())
        aggregationForGroup.set(extension.log.aggregation.get().toKover())
        if (extension.log.header.isPresent) {
            header.set(extension.log.header)
        }
    }

    binary {
        onCheck.set(extension.binary.onCheck)
        if (extension.binary.file.isPresent) {
            file.set(extension.binary.file)
        }
    }
}

/**
 * Registers one verification rule per configured shorthand bound, plus every named rule.
 *
 * A shorthand bound that was never set registers no rule at all. Registering one with a minimum
 * of zero would produce a gate that passes unconditionally, which is indistinguishable from a
 * working gate right up until the day it should have caught something.
 *
 * @param extension The Kreate verification configuration.
 * @since 2.2.0
 */
private fun KoverVerifyTaskConfig.configureVerification(extension: CoverageVerifyExtension) {
    onCheck.set(extension.runOnCheck)
    warningInsteadOfFailure.set(extension.warningInsteadOfFailure)

    val defaultGrouping = extension.groupBy.get().toKover()

    fun registerShorthand(name: String, minimum: Int, unit: KoverCoverageUnit) {
        rule(name) {
            groupBy.set(defaultGrouping)
            minBound(minimum, unit, KoverAggregationType.COVERED_PERCENTAGE)
        }
    }

    if (extension.minLineCoverage.isPresent) {
        registerShorthand("Minimum line coverage", extension.minLineCoverage.get(), KoverCoverageUnit.LINE)
    }

    if (extension.minBranchCoverage.isPresent) {
        registerShorthand("Minimum branch coverage", extension.minBranchCoverage.get(), KoverCoverageUnit.BRANCH)
    }

    if (extension.minInstructionCoverage.isPresent) {
        registerShorthand(
            "Minimum instruction coverage",
            extension.minInstructionCoverage.get(),
            KoverCoverageUnit.INSTRUCTION
        )
    }

    extension.rules.forEach { spec -> registerRule(spec, defaultGrouping) }
}

/**
 * Registers one named rule and each of its bounds.
 *
 * @param spec The Kreate rule configuration.
 * @param defaultGrouping The grouping used when the rule does not override it.
 * @throws GradleException If a bound sets neither a minimum nor a maximum.
 * @since 2.2.0
 */
private fun KoverVerifyTaskConfig.registerRule(
    spec: CoverageRuleSpec,
    defaultGrouping: KoverGroupingEntityType
) {
    val specBounds = spec.bounds.get()
    if (specBounds.isEmpty()) {
        throw GradleException(
            "Coverage rule '${spec.name}' declares no bounds. A rule without a bound checks " +
                "nothing and always passes; give it a `bound { }`, `minBound(...)` or " +
                "`maxBound(...)`, or remove it."
        )
    }

    rule(spec.name) {
        disabled.set(spec.disabled)
        groupBy.set(spec.groupBy.orElse(defaultGrouping.toKreate()).map { it.toKover() })

        specBounds.forEach { boundSpec ->
            if (!boundSpec.min.isPresent && !boundSpec.max.isPresent) {
                throw GradleException(
                    "A bound of coverage rule '${spec.name}' sets neither `min` nor `max`. " +
                        "Such a bound measures something and demands nothing."
                )
            }

            bound {
                minValue.set(boundSpec.min)
                maxValue.set(boundSpec.max)
                coverageUnits.set(boundSpec.unit.get().toKover())
                aggregationForGroup.set(boundSpec.aggregation.get().toKover())
            }
        }
    }
}

/**
 * Merges the coverage of the configured projects into this project's reports.
 *
 * Kover's own `merge { }` block applies the Kover plugin to the projects it aggregates. Kreate
 * uses the `kover` configuration directly instead, so that no project gains a plugin its build
 * script never asked for, and reports a missing plugin by project path rather than leaving Gradle
 * to fail with a variant resolution error.
 *
 * @param extension The Kreate aggregation configuration.
 * @throws GradleException If a configured path names no project, or names one without Kover.
 * @since 2.2.0
 */
private fun Project.configureAggregation(extension: CoverageAggregateExtension) {
    if (!extension.enabled.get()) {
        return
    }

    val configured = extension.projects.get()
    val targets = if (configured.isEmpty()) {
        subprojects.toList()
    } else {
        configured.map { path ->
            findProject(path) ?: throw GradleException(
                "Kreate's coverage aggregation on project '${this.path}' lists project '$path', " +
                    "which does not exist in this build."
            )
        }
    }

    targets.forEach { target ->
        dependencies.add(KOVER_CONFIGURATION, target)

        // An aggregated project has to measure its own coverage before it can contribute any, so
        // it needs the plugin. Applied here rather than demanded of each subproject's build
        // script, and applied now rather than checked later: Gradle evaluates the root before its
        // children, so this runs before the subproject is configured, which is the only point at
        // which applying a plugin to it still means anything.
        target.pluginManager.apply(KOVER_PLUGIN_ID)
    }
}

/**
 * Maps a Kreate coverage unit onto Kover's equivalent.
 *
 * @return The matching Kover coverage unit.
 * @since 2.2.0
 */
private fun CoverageUnit.toKover(): KoverCoverageUnit = when (this) {
    CoverageUnit.LINE -> KoverCoverageUnit.LINE
    CoverageUnit.INSTRUCTION -> KoverCoverageUnit.INSTRUCTION
    CoverageUnit.BRANCH -> KoverCoverageUnit.BRANCH
}

/**
 * Maps a Kreate aggregation type onto Kover's equivalent.
 *
 * @return The matching Kover aggregation type.
 * @since 2.2.0
 */
private fun Aggregation.toKover(): KoverAggregationType = when (this) {
    Aggregation.COVERED_COUNT -> KoverAggregationType.COVERED_COUNT
    Aggregation.MISSED_COUNT -> KoverAggregationType.MISSED_COUNT
    Aggregation.COVERED_PERCENTAGE -> KoverAggregationType.COVERED_PERCENTAGE
    Aggregation.MISSED_PERCENTAGE -> KoverAggregationType.MISSED_PERCENTAGE
}

/**
 * Maps a Kreate grouping entity onto Kover's equivalent.
 *
 * @return The matching Kover grouping entity type.
 * @since 2.2.0
 */
private fun Grouping.toKover(): KoverGroupingEntityType = when (this) {
    Grouping.APPLICATION -> KoverGroupingEntityType.APPLICATION
    Grouping.CLASS -> KoverGroupingEntityType.CLASS
    Grouping.PACKAGE -> KoverGroupingEntityType.PACKAGE
}

/**
 * Maps a Kover grouping entity back onto Kreate's equivalent.
 *
 * @return The matching Kreate grouping.
 * @since 2.2.0
 */
private fun KoverGroupingEntityType.toKreate(): Grouping = when (this) {
    KoverGroupingEntityType.APPLICATION -> Grouping.APPLICATION
    KoverGroupingEntityType.CLASS -> Grouping.CLASS
    KoverGroupingEntityType.PACKAGE -> Grouping.PACKAGE
}
