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

package com.davils.kreate.module.project.trivy.tasks

import com.davils.kreate.jobs.Task
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class TrivyCVEScan @Inject constructor(
    private val exec: ExecOperations
) : Task(
    "Scans for CVEs in the project dependencies.",
    "kreate trivy"
) {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val lockFiles: ConfigurableFileCollection

    @get:Input
    public abstract val severity: Property<String>

    @get:Input
    public abstract val failOnFindings: Property<Boolean>

    @TaskAction
    override fun execute() {
        if (lockFiles.isEmpty) {
            logger.warn("No *.gradle.lockfile found — run './gradlew updateDependencyLocks' first.")
            return
        }

        var foundCves = false

        lockFiles.forEach { lockFile ->
            val result = exec.exec {
                isIgnoreExitValue = true
                commandLine(
                    "trivy", "fs",
                    "--scanners", "vuln",
                    "--severity", severity.get(),
                    "--exit-code", if (failOnFindings.get()) "1" else "0",
                    "--format", "table",
                    lockFile.absolutePath
                )
            }
            if (result.exitValue == 1) foundCves = true
        }

        if (foundCves && failOnFindings.get()) {
            throw GradleException("Trivy found CVEs in dependencies!")
        }
    }
}