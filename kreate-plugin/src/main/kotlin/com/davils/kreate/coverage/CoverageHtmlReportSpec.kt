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

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The HTML coverage report.
 *
 * @param factory The object factory used for creating properties.
 * @param project The project instance used to resolve paths.
 * @since 2.2.0
 */
public abstract class CoverageHtmlReportSpec @Inject constructor(
    factory: ObjectFactory,
    project: Project
) : CoverageReportSpec(factory) {
    /**
     * The directory the HTML report is written to.
     *
     * Defaults to `build/reports/kover/html`.
     *
     * @since 2.2.0
     */
    public val directory: DirectoryProperty = factory.directoryProperty().convention(
        project.layout.buildDirectory.dir("reports/kover/html")
    )

    /**
     * The title shown in the generated pages.
     *
     * Unset by default, which leaves the coverage engine's own title in place.
     *
     * @since 2.2.0
     */
    public val title: Property<String> = factory.property(String::class.java)

    /**
     * The character set the pages are written in.
     *
     * Unset by default.
     *
     * @since 2.2.0
     */
    public val charset: Property<String> = factory.property(String::class.java)
}
