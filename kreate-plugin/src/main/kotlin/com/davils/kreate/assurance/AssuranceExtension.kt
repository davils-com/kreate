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
import javax.inject.Inject

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
    private val ignoreRules: MutableMap<String, AssuranceIgnoreSpec> = linkedMapOf()

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
     * Finding documents written by other tools, which the gate treats like its own sources.
     *
     * @since 4.0.0
     */
    public val externalFindings: ConfigurableFileCollection = factory.fileCollection()

    /**
     * Configuration of the baseline.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val baseline: AssuranceBaselineExtension

    /**
     * Configuration of the reports.
     *
     * @since 4.0.0
     */
    @get:Nested
    public abstract val reports: AssuranceReportsExtension

    /**
     * Accepts the findings with the given rule id, advisory id or alias.
     *
     * @param id The rule id, advisory id or alias.
     * @param action The configuration of the rule.
     * @since 4.0.0
     */
    public fun ignore(id: String, action: Action<AssuranceIgnoreSpec>) {
        val spec = ignoreRules.getOrPut(id) { AssuranceIgnoreSpec() }
        action.execute(spec)
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

    internal fun declaredIgnoreRules(): Map<String, AssuranceIgnoreSpec> = ignoreRules.toMap()
}
