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

import com.davils.kreate.module.project.trivy.Severity
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class TrivyLicenseExtension @Inject constructor(factory: ObjectFactory, project: Project) {
    public val severity: ListProperty<Severity> = factory.listProperty(
        Severity::class.java
    ).convention(listOf(Severity.CRITICAL, Severity.HIGH, Severity.UNKNOWN))

    public val failOnForbidden: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(true)

    public val fullLicenseScan: Property<Boolean> = factory.property(
        Boolean::class.java
    ).convention(false)

    public val ignoredLicenses: ListProperty<String> = factory.listProperty(
        String::class.java
    ).convention(emptyList())

    public val lockFiles: ConfigurableFileCollection = factory.fileCollection().from(
        project.fileTree(project.projectDir) {
            include("*.lockfile")
        }
    )
}
