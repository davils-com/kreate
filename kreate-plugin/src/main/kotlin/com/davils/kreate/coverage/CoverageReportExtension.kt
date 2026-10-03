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
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Configuration for the coverage reports.
 *
 * Each format answers a different question. HTML is for a person looking for the untested branch;
 * XML is for a machine — a CI system rendering merge request annotations, or a quality dashboard.
 * The log report is for the build output itself, which is where a CI system reads the headline
 * percentage from.
 *
 * @since 2.2.0
 */
public abstract class CoverageReportExtension @Inject constructor() {
    /**
     * Configuration for the XML report.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val xml: CoverageXmlReportSpec

    /**
     * Configuration for the HTML report.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val html: CoverageHtmlReportSpec

    /**
     * Configuration for the coverage summary printed to the build log.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val log: CoverageLogReportSpec

    /**
     * Configuration for the binary report.
     *
     * @since 2.2.0
     */
    @get:Nested
    public abstract val binary: CoverageBinaryReportSpec

    /**
     * Configures the XML report.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun xml(action: Action<CoverageXmlReportSpec>) {
        action.execute(xml)
    }

    /**
     * Configures the HTML report.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun html(action: Action<CoverageHtmlReportSpec>) {
        action.execute(html)
    }

    /**
     * Configures the log report.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun log(action: Action<CoverageLogReportSpec>) {
        action.execute(log)
    }

    /**
     * Configures the binary report.
     *
     * @param action The configuration action.
     * @since 2.2.0
     */
    public fun binary(action: Action<CoverageBinaryReportSpec>) {
        action.execute(binary)
    }
}
