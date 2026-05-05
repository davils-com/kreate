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

package com.davils.kreate.module.project.trivy.extension

import com.davils.kreate.module.project.trivy.SecretSeverity
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring secret scanning with Trivy.
 *
 * This extension allows defining which files to scan for secrets and how to handle findings.
 *
 * @param factory The object factory used for creating properties.
 * @param project The Gradle project instance.
 * @since 1.2.0
 */
public abstract class TrivySecretExtension @Inject constructor(factory: ObjectFactory, project: Project) {
    /**
     * The list of secret severities to report (e.g., CRITICAL, HIGH).
     *
     * @since 1.2.0
     */
    public val severity: ListProperty<SecretSeverity> = factory.listProperty(
        SecretSeverity::class.java
    ).convention(listOf(
        SecretSeverity.CRITICAL, SecretSeverity.HIGH, SecretSeverity.MEDIUM, SecretSeverity.LOW
    ))

    /**
     * Whether the task should fail the build if any secrets are found.
     *
     * @since 1.2.0
     */
    public val failOnFindings: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(true)

    /**
     * The configuration file for Trivy secret scanning.
     *
     * @since 1.2.0
     */
    public val secretConfig: RegularFileProperty = factory.fileProperty().convention(
        project.rootProject.layout.projectDirectory.file("trivy-secret.yaml")
    )

    /**
     * The collection of source files to scan for secrets.
     *
     * @since 1.2.0
     */
    public val sourceFiles: ConfigurableFileCollection = factory.fileCollection().from(
        project.fileTree(project.projectDir) {
            include(
                "src/**/*.kt",
                "src/**/*.java",
                "**/*.yaml",
                "**/*.yml",
                "**/*.env",
                "**/*.properties",
                "**/*.json"
            )
        }
    )
}
