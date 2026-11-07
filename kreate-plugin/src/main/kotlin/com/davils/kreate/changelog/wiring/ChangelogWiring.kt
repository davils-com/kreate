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
package com.davils.kreate.changelog.wiring

import com.davils.kreate.changelog.ChangelogExtension
import com.davils.kreate.changelog.ChangelogTaskNames
import com.davils.kreate.changelog.task.ChangelogCheck
import com.davils.kreate.changelog.task.ChangelogGenerate
import com.davils.kreate.changelog.task.ChangelogNotes
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val CHECK_REPORT: String = "kreate/changelog/check.txt"

private const val NEXT_SECTION: String = "kreate/changelog/next.md"

private const val NOTES: String = "kreate/changelog/notes.md"

internal fun Project.registerChangelogTasks(changelog: ChangelogExtension) {
    val owner = this
    val version = provider { owner.version.toString() }

    val check = tasks.register<ChangelogCheck>(ChangelogTaskNames.CHECK) {
        this.changelog.from(changelog.file)
        projectVersion.set(version)
        reportFile.set(owner.layout.buildDirectory.file(CHECK_REPORT))
    }

    tasks.register<ChangelogGenerate>(ChangelogTaskNames.GENERATE) {
        this.changelog.set(changelog.file)
        projectDirectory.set(owner.layout.projectDirectory)
        projectVersion.set(version)
        tagPrefix.set(changelog.tagPrefix)
        includeDates.set(changelog.includeDates)
        outputFile.set(owner.layout.buildDirectory.file(NEXT_SECTION))
    }

    tasks.register<ChangelogNotes>(ChangelogTaskNames.NOTES) {
        this.changelog.set(changelog.file)
        projectVersion.set(version)
        outputFile.set(owner.layout.buildDirectory.file(NOTES))
    }

    if (!changelog.runOnCheck.get()) return
    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach { dependsOn(check) }
}
