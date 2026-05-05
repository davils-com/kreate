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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

/**
 * Extension for configuring Detekt static analysis.
 *
 * This extension provides options to enable/disable Detekt, configure rule sets,
 * and define report settings.
 *
 * @since 1.2.0
 */
public abstract class DetektExtension @Inject constructor(factory: ObjectFactory, project: Project) {

    /**
     * Whether Detekt is enabled for this project.
     *
     * @since 1.2.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * Whether to enable all Detekt rules, including experimental ones.
     *
     * @since 1.2.0
     */
    public val allRules: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Whether to build upon the default Detekt configuration.
     *
     * @since 1.2.0
     */
    public val buildUponDefaultConfig: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The Detekt configuration file.
     *
     * Defaults to `detekt.yaml` in the root project directory.
     *
     * @since 1.2.0
     */
    public val config: RegularFileProperty = factory.fileProperty().convention(
        project.rootProject.layout.projectDirectory.file("detekt.yaml")
    )

    /**
     * Configuration for Detekt reports.
     *
     * @since 1.2.0
     */
    @get:Nested
    public abstract val reports: DetektReportExtension

    /**
     * Configures the Detekt reports.
     *
     * @param action The configuration action.
     * @since 1.2.0
     */
    public fun reports(action: Action<DetektReportExtension>) {
        action.execute(reports)
    }
}
