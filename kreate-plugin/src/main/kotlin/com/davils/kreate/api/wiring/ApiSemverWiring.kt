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
package com.davils.kreate.api.wiring

import com.davils.kreate.api.ApiValidationExtension
import com.davils.kreate.api.ApiValidationTaskNames
import com.davils.kreate.api.task.ApiSemverCheck
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val SEMVER_REPORT: String = "kreate/api/semver.txt"

internal fun Project.registerApiSemverCheck(apiExtension: ApiValidationExtension) {
    val semver = apiExtension.semver
    if (!semver.enabled.get()) return
    val owner = this

    val semverCheck = tasks.register<ApiSemverCheck>(ApiValidationTaskNames.SEMVER_CHECK) {
        dumpFile.set(apiExtension.apiDirectory.file(apiExtension.dumpFileName))
        projectDirectory.set(owner.layout.projectDirectory)
        projectVersion.set(owner.provider { owner.version.toString() })
        baseline.set(semver.baseline)
        tagPrefix.set(semver.tagPrefix)
        requireBreakingCommit.set(semver.requireBreakingCommit)
        reportFile.set(owner.layout.buildDirectory.file(SEMVER_REPORT))
        mustRunAfter(ApiValidationTaskNames.CHECK)
    }

    if (!semver.runOnCheck.get()) return
    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach { dependsOn(semverCheck) }
}
