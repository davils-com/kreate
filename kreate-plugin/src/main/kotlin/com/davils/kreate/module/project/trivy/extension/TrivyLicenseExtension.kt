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

import com.davils.kreate.module.project.trivy.LicenseSeverity
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring license scanning with Trivy.
 *
 * @param factory The object factory used for creating properties.
 * @param project The Gradle project instance.
 * @since 1.2.0
 */
public abstract class TrivyLicenseExtension @Inject constructor(factory: ObjectFactory, project: Project) {

    /**
     * The list of license severities to check for (e.g., CRITICAL, HIGH).
     *
     * @since 1.2.0
     */
    public val severity: ListProperty<LicenseSeverity> = factory.listProperty(
        LicenseSeverity::class.java
    ).convention(listOf(LicenseSeverity.CRITICAL, LicenseSeverity.HIGH, LicenseSeverity.UNKNOWN))

    /**
     * Whether the task should fail if forbidden or restricted licenses are found.
     *
     * @since 1.2.0
     */
    public val failOnForbidden: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(true)

    /**
     * Whether to enable a full license scan.
     *
     * @since 1.2.0
     */
    public val fullLicenseScan: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    /**
     * Licenses to be ignored during the scan.
     *
     * @since 1.2.0
     */
    public val ignoredLicenses: ListProperty<String> = factory.listProperty(
        String::class.java
    ).convention(emptyList())

    /**
     * The collection of lock files to be scanned for license issues.
     *
     * @since 1.2.0
     */
    public val lockFiles: ConfigurableFileCollection = factory.fileCollection().from(
        project.fileTree(project.projectDir) {
            include("*.lockfile")
        }
    )
}
