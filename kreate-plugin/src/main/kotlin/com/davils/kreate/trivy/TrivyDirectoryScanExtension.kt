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
package com.davils.kreate.trivy

import com.davils.kreate.assurance.Severity
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of a Trivy scan that reads the whole project directory.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.0.0
 */
public abstract class TrivyDirectoryScanExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * Whether the scan runs. Defaults to `false`.
     *
     * @since 4.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The severities to report.
     *
     * @since 4.0.0
     */
    public val severity: ListProperty<Severity> = factory.listProperty(Severity::class.java)
        .convention(listOf(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW))

    /**
     * Whether the task should fail if anything is found.
     *
     * @since 4.0.0
     */
    public val failOnFindings: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * Directories the scan skips, relative to the project. Defaults to Gradle's and Kotlin's own.
     *
     * @since 4.0.0
     */
    public val skipDirectories: ListProperty<String> = factory.listProperty(String::class.java)
        .convention(listOf("build", ".gradle", ".kotlin", ".git"))
}
