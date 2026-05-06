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

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Specification for a Detekt report.
 *
 * @param factory The object factory used to create properties.
 * @since 1.2.0
 */
public abstract class DetektReportSpec @Inject constructor(factory: ObjectFactory) {

    /**
     * Whether this report is required.
     *
     * @since 1.2.0
     */
    public val required: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The output location for this report.
     *
     * If not specified, a default location under the build directory will be used.
     *
     * @since 1.2.0
     */
    public val outputLocation: RegularFileProperty = factory.fileProperty()
}

/**
 * Specification for a Detekt Checkstyle report.
 *
 * @param factory The object factory used to create properties.
 * @param project The project instance used to resolve paths.
 * @since 1.2.2
 */
public abstract class DetektCheckstyleReportSpec @Inject constructor(
    factory: ObjectFactory,
    project: Project,
) : DetektReportSpec(factory) {
    init {
        outputLocation.convention(project.layout.buildDirectory.file("reports/detekt/detekt.xml"))
    }
}

/**
 * Specification for a Detekt HTML report.
 *
 * @param factory The object factory used to create properties.
 * @param project The project instance used to resolve paths.
 * @since 1.2.2
 */
public abstract class DetektHtmlReportSpec @Inject constructor(
    factory: ObjectFactory,
    project: Project,
) : DetektReportSpec(factory) {
    init {
        required.convention(true)
        outputLocation.convention(project.layout.buildDirectory.file("reports/detekt/detekt.html"))
    }
}

/**
 * Specification for a Detekt Markdown report.
 *
 * @param factory The object factory used to create properties.
 * @param project The project instance used to resolve paths.
 * @since 1.2.2
 */
public abstract class DetektMarkdownReportSpec @Inject constructor(
    factory: ObjectFactory,
    project: Project,
) : DetektReportSpec(factory) {
    init {
        required.convention(true)
        outputLocation.convention(project.layout.buildDirectory.file("reports/detekt/detekt.md"))
    }
}

/**
 * Specification for a Detekt SARIF report.
 *
 * @param factory The object factory used to create properties.
 * @param project The project instance used to resolve paths.
 * @since 1.2.2
 */
public abstract class DetektSarifReportSpec @Inject constructor(
    factory: ObjectFactory,
    project: Project,
) : DetektReportSpec(factory) {
    init {
        outputLocation.convention(project.layout.buildDirectory.file("reports/detekt/detekt.sarif"))
    }
}
