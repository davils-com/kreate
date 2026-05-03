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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class TrivyLicenseScan @Inject constructor(
    private val exec: ExecOperations
) : Task(
    "Scans source files for licenses using Trivy",
    "kreate trivy"
) {

    @get:InputDirectory
    public abstract val targetDir: DirectoryProperty

    @get:Input
    public abstract val failOnForbidden: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val lockFiles: ConfigurableFileCollection

    @TaskAction
    override fun execute() {
        logger.lifecycle("Running Trivy license scan on ${targetDir.get().asFile.absolutePath} with lock files: ${lockFiles.files.joinToString(", ")}")
        if (lockFiles.isEmpty) {
            logger.warn("No *.gradle.lockfile found — skipping license scan. Run 'dependencies --write-locks' first.")
            return
        }


        val result = exec.exec {
            isIgnoreExitValue = true
            commandLine(
                "trivy", "fs",
                "--scanners", "license",
                "--exit-code", if (failOnForbidden.get()) "1" else "0",
                "--severity", "UNKNOWN,HIGH,CRITICAL,LOW",
                "--format", "table",
                targetDir.get().asFile.absolutePath
            )
        }

        if (result.exitValue == 1 && failOnForbidden.get()) {
            throw GradleException("Trivy found forbidden or restricted licenses in dependencies!")
        }
    }
}