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

package com.davils.kreate.license.wiring

import com.davils.kreate.gradle.GENERATED_PATHS
import com.davils.kreate.license.LicenseHeaderExtension
import com.davils.kreate.license.LicenseHeaderTaskNames
import com.davils.kreate.license.task.LicenseHeaderCheck
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val REPORT: String = "kreate/license/headers.txt"

internal fun Project.registerLicenseHeaderCheck(licenseHeader: LicenseHeaderExtension) {
    if (!licenseHeader.notice.isPresent) {
        throw InvalidUserDataException(
            "kreate { project { licenseHeader { } } } is enabled but has no notice. " +
                "Set notice or call apacheLicense(\"Copyright <year> <holder>\")."
        )
    }
    val owner = this
    val includes = licenseHeader.includes.get()
    val excludes = GENERATED_PATHS + licenseHeader.excludes.get()
    val selected = fileTree(layout.projectDirectory) {
        include(includes)
        exclude(excludes)
    }

    val check = tasks.register<LicenseHeaderCheck>(LicenseHeaderTaskNames.CHECK) {
        sources.from(selected)
        notice.set(licenseHeader.notice)
        baseDirectory.set(owner.layout.projectDirectory)
        reportFile.set(owner.layout.buildDirectory.file(REPORT))
    }

    if (!licenseHeader.runOnCheck.get()) return
    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach { dependsOn(check) }
}
