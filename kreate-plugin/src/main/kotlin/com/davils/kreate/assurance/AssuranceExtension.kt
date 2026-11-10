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
package com.davils.kreate.assurance

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested
import java.time.Duration
import javax.inject.Inject

private const val DEFAULT_ADVISORY_AGE_DAYS: Long = 3

private const val DEFAULT_REUSE_HOURS: Long = 6

/**
 * Configuration of the software assurance gate.
 *
 * The gate collects findings from every source, filters them through the ignore rules and the
 * baseline, writes one report and then decides. A check that could not finish is never reported as
 * clean, and nothing here can hide that.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class AssuranceExtension @Inject constructor(factory: ObjectFactory) {
    private val ignoreRules: MutableList<Pair<String, AssuranceIgnoreSpec>> = mutableListOf()

    /**
     * Whether the gate is enabled for this project. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether `check` runs the gate. Defaults to `true`.
     *
     * @since 4.0.0
     */
    public val runOnCheck: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The severity from which on a finding fails the build. Defaults to [Severity.HIGH].
     *
     * An advisory without a rating counts as [Severity.HIGH], because an unrated vulnerability is not
     * a harmless one.
     *
     * @since 4.0.0
     */
    public val failOn: Property<Severity> = factory.property(Severity::class.java).convention(Severity.HIGH)

    /**
     * The severity below which a finding is dropped. Defaults to [Severity.UNKNOWN], which drops none.
     *
     * @since 4.0.0
     */
    public val minSeverity: Property<Severity> = factory.property(Severity::class.java).convention(Severity.UNKNOWN)

    /**
     * Whether findings are only reported. Defaults to `false`.
     *
     * An incomplete check still fails: this hides findings, never a missing verdict.
     *
     * @since 4.0.0
     */
    public val reportOnly: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The severities neither an ignore rule nor the baseline may hide.
     *
     * @since 4.0.0
     */
    public val forbidIgnoreOf: SetProperty<Severity> = factory.setProperty(Severity::class.java)

    /**
     * Groups of this organization. Their modules, and those of their subgroups, are never sent to a
     * public service, because the name of an internal module is itself worth protecting.
     *
     * @since 4.0.0
     */
    public val internalGroups: SetProperty<String> = factory.setProperty(String::class.java)

    /**
     * How old the advisory data of a result may be. Defaults to 3 days. Older data is incomplete.
     *
     * @since 4.0.0
     */
    public val maxAdvisoryAge: Property<Duration> =
        factory.property(Duration::class.java).convention(Duration.ofDays(DEFAULT_ADVISORY_AGE_DAYS))

    /**
     * How long an audit of unchanged dependencies stands before it runs again. Defaults to 6 hours.
     *
     * @since 4.0.0
     */
    public val reuseVerdictFor: Property<Duration> =
        factory.property(Duration::class.java).convention(Duration.ofHours(DEFAULT_REUSE_HOURS))

    /**
     * Finding documents written by other tools, which the gate treats like its own sources.
     *
     * @since 4.0.0
     */
    public val externalFindings: ConfigurableFileCollection = factory.fileCollection()

    /**
     * Configuration of the audit against OSV.dev.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val advisories: AssuranceAdvisoriesExtension

    /**
     * Configuration of the inspection of what the dependencies contain.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val inspection: AssuranceInspectionExtension

    /**
     * Configuration of the baseline.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val baseline: AssuranceBaselineExtension

    /**
     * Configuration of the trust assessment through deps.dev.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val trust: AssuranceTrustExtension

    /**
     * Configuration of the checks for typosquatting and dependency confusion.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val confusion: AssuranceConfusionExtension

    /**
     * Configuration of the dependency policy.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val dependencyPolicy: AssuranceDependencyPolicyExtension

    /**
     * Configuration of the workspace check.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val workspace: AssuranceWorkspaceExtension

    /**
     * Configuration of the aggregation over the projects of a build.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val aggregate: AssuranceAggregateExtension

    /**
     * Configuration of the reports.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val reports: AssuranceReportsExtension

    /**
     * Accepts the findings with the given rule id, advisory id or alias. Every call declares a rule of
     * its own, so one id can be accepted for several modules, each with its own reason and expiry.
     *
     * @param id The rule id, advisory id or alias.
     * @param action The configuration of the rule.
     * @since 4.0.0
     */
    public fun ignore(id: String, action: Action<AssuranceIgnoreSpec>) {
        val spec = AssuranceIgnoreSpec()
        action.execute(spec)
        ignoreRules += id to spec
    }

    /**
     * Configures the audit against OSV.dev.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun advisories(action: Action<AssuranceAdvisoriesExtension>) {
        action.execute(advisories)
    }

    /**
     * Configures the trust assessment through deps.dev.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun trust(action: Action<AssuranceTrustExtension>) {
        action.execute(trust)
    }

    /**
     * Configures the checks for typosquatting and dependency confusion.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun confusion(action: Action<AssuranceConfusionExtension>) {
        action.execute(confusion)
    }

    /**
     * Configures the aggregation over the projects of a build.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun aggregate(action: Action<AssuranceAggregateExtension>) {
        action.execute(aggregate)
    }

    /**
     * Configures the workspace check.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun workspace(action: Action<AssuranceWorkspaceExtension>) {
        action.execute(workspace)
    }

    /**
     * Configures the dependency policy.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun dependencyPolicy(action: Action<AssuranceDependencyPolicyExtension>) {
        action.execute(dependencyPolicy)
    }

    /**
     * Configures the inspection of what the dependencies contain.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun inspection(action: Action<AssuranceInspectionExtension>) {
        action.execute(inspection)
    }

    /**
     * Configures the baseline.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun baseline(action: Action<AssuranceBaselineExtension>) {
        action.execute(baseline)
    }

    /**
     * Configures the reports.
     *
     * @param action The configuration action.
     * @since 4.0.0
     */
    public fun reports(action: Action<AssuranceReportsExtension>) {
        action.execute(reports)
    }

    internal fun declaredIgnoreRules(): List<Pair<String, AssuranceIgnoreSpec>> = ignoreRules.toList()
}
