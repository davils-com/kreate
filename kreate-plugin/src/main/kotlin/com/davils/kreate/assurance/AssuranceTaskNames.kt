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

/**
 * The names of the tasks the assurance feature registers.
 *
 * @since 4.0.0
 */
public object AssuranceTaskNames {
    /**
     * Decides whether the build passes the gate.
     *
     * @since 4.0.0
     */
    public const val CHECK: String = "kreateAssurance"

    /**
     * Writes the assurance reports. Never fails on findings.
     *
     * @since 4.0.0
     */
    public const val REPORT: String = "kreateAssuranceReport"

    /**
     * Records the current findings in the baseline.
     *
     * @since 4.0.0
     */
    public const val BASELINE_CREATE: String = "kreateAssuranceBaselineCreate"

    /**
     * Removes what no longer occurs from the baseline. Never adds anything.
     *
     * @since 4.0.0
     */
    public const val BASELINE_PRUNE: String = "kreateAssuranceBaselinePrune"

    /**
     * Shows the effective assurance configuration and what differs from the defaults.
     *
     * @since 4.0.0
     */
    public const val CONFIG: String = "kreateAssuranceConfig"

    /**
     * Checks the assurance configuration for settings that weaken the gate.
     *
     * @since 4.0.0
     */
    public const val CONFIG_LINT: String = "kreateAssuranceConfigLint"

    /**
     * Explains a rule of the catalog, or lists all rules.
     *
     * @since 4.0.0
     */
    public const val EXPLAIN: String = "kreateExplain"

    /**
     * Puts the gates of all projects of the build into one report. Only in the root project, with
     * `aggregate { enabled }`.
     *
     * @since 4.0.0
     */
    public const val AGGREGATE: String = "kreateAssuranceAggregate"
}
