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

package com.davils.kreate.module.project.detekt.extension

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Detekt reports.
 *
 * This class allows enabling and specifying output locations for various Detekt report formats.
 *
 * @since 1.2.0
 */
public abstract class DetektReportExtension @Inject constructor(factory: ObjectFactory, project: Project) {

    /**
     * Configuration for the Checkstyle report.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val checkstyle: DetektReportSpec

    /**
     * Configuration for the HTML report.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val html: DetektReportSpec

    /**
     * Configuration for the Markdown report.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val markdown: DetektReportSpec

    /**
     * Configuration for the SARIF report.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val sarif: DetektReportSpec


    /**
     * Configures the Checkstyle report.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun checkstyle(action: Action<DetektReportSpec>) {
        action.execute(checkstyle)
    }

    /**
     * Configures the HTML report.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun html(action: Action<DetektReportSpec>) {
        action.execute(html)
    }

    /**
     * Configures the Markdown report.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun markdown(action: Action<DetektReportSpec>) {
        action.execute(markdown)
    }

    /**
     * Configures the SARIF report.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun sarif(action: Action<DetektReportSpec>) {
        action.execute(sarif)
    }
}
