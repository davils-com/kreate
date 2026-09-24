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

package com.davils.kreate.module.trivy.extension

import com.davils.kreate.module.trivy.SecretSeverity
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
     * The default covers the sources and the configuration files a credential is actually pasted
     * into: Kotlin and Java under `src`, and YAML, `.env`, properties and JSON anywhere in the
     * project, which is what reaches a build script, a version catalog and a deployment descriptor.
     *
     * **Generated output is excluded, and that is not only about noise.** A secret under `build` is a
     * copy of one in a file this scan already reads, so reporting it twice adds no finding and costs
     * the time it takes to scan every artifact in the directory. The harder reason is that Gradle
     * refuses a build whose task declares an input overlapping another task's output: with `build`
     * left in, `kreateTrivySecretScan` in the same invocation as any task that writes a matching file
     * there - a `processResources`, a Kotlin compilation's caches - fails with a message about an
     * undeclared dependency rather than about secrets. `.gradle` and `.kotlin` are excluded for the
     * same reason.
     *
     * **`from` adds to this default; only `setFrom` replaces it.** That is Gradle's semantics for
     * every file collection, and it is worth spelling out here because the mistake is invisible: a
     * project that narrows the scan with `sourceFiles.from(fileTree(projectDir) { exclude(...) })`
     * still scans everything the default matched, since its own excludes apply to its own tree and
     * not to the default one beside it. Use `setFrom` to state the whole scope, and `from` only to
     * add to it.
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
            exclude(
                "**/build/**",
                "**/.gradle/**",
                "**/.kotlin/**"
            )
        }
    )
}
