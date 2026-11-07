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
package com.davils.kreate.changelog.task

import com.davils.kreate.changelog.document.ChangelogDocument
import com.davils.kreate.changelog.document.ChangelogProblem
import com.davils.kreate.changelog.document.ChangelogValidator
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.vcs.releaseVersionOf
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationException

@CacheableTask
internal abstract class ChangelogCheck : KreateTask(
    "Checks the structure of the changelog and that the project version has a section.",
    KreateTaskGroup.KREATE
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract val changelog: ConfigurableFileCollection

    @get:Input
    public abstract val projectVersion: Property<String>

    @get:OutputFile
    public abstract val reportFile: RegularFileProperty

    @TaskAction
    public fun check() {
        val file = changelog.singleOrNull { candidate -> candidate.isFile }
        val version = releaseVersionOf(projectVersion.get())?.toString()
        val problems = file?.let { existing ->
            ChangelogValidator.validate(ChangelogDocument.parse(existing.readText()), version)
        } ?: listOf(ChangelogProblem("The changelog does not exist."))
        val name = file?.name ?: changelog.files.firstOrNull()?.name.orEmpty()
        val lines = problems.map { problem -> lineOf(name, problem) }
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(lines.joinToString("\n", postfix = "\n"))
        if (problems.isEmpty()) return logger.lifecycle("$name is valid.")
        throw VerificationException("$name has ${problems.size} problem(s):\n" + lines.joinToString("\n"))
    }

    private fun lineOf(name: String, problem: ChangelogProblem): String {
        val line = problem.line?.let { number -> ":$number" }.orEmpty()
        return "$name$line: ${problem.message}"
    }
}
