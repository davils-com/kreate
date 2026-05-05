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
import com.davils.kreate.module.project.trivy.resolveTrivyCommand
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Scans lock files for licenses using Trivy.
 *
 * This task iterates over all provided lock files and uses Trivy to scan them
 * for forbidden or restricted licenses based on the configured severity levels.
 *
 * @param exec The Gradle ExecOperations used to run the Trivy process.
 * @since 1.2.0
 */
@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
public abstract class TrivyLicenseScan @Inject constructor(
    private val exec: ExecOperations
) : Task(
    "Scans source files for licenses using Trivy",
    "kreate trivy"
) {
    /**
     * Whether the task should fail if forbidden or restricted licenses are found.
     *
     * @since 1.2.0
     */
    @get:Input
    public abstract val failOnForbidden: Property<Boolean>

    /**
     * The list of license severities to check for (e.g., HIGH, CRITICAL, UNKNOWN).
     *
     * @since 1.2.0
     */
    @get:Input
    public abstract val severity: ListProperty<String>

    /**
     * Licenses to be ignored during the scan.
     *
     * @since 1.2.0
     */
    @get:Input
    public abstract val ignoredLicenses: ListProperty<String>

    /**
     * The collection of lock files to be scanned for license issues.
     *
     * @since 1.2.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val lockFiles: ConfigurableFileCollection

    /**
     * Executes the license scan for each lock file in the collection.
     *
     * Runs the Trivy CLI in filesystem mode specifically for license scanning.
     * Throws a [GradleException] if forbidden licenses are found and [failOnForbidden] is set to true.
     *
     * @since 1.2.0
     */
    @TaskAction
    override fun execute() {
        if (lockFiles.isEmpty) {
            logger.lifecycle("No lock files found. Skipping license scan. Run 'gradle dependencies --write-locks'.")
            return
        }

        var isForbidden = false
        lockFiles.forEach { file ->
            val result = exec.exec {
                isIgnoreExitValue = true
                val trivyCmd = resolveTrivyCommand()
                val args = mutableListOf(
                    trivyCmd, "fs",
                    "--scanners", "license",
                    "--exit-code", if (failOnForbidden.get()) "1" else "0",
                    "--severity", severity.get().joinToString(","),
                    "--format", "table",
                )

                if (ignoredLicenses.get().isNotEmpty()) {
                    args.add("--ignored-licenses")
                    args.add(ignoredLicenses.get().joinToString(","))
                }

                args.add(file.absolutePath)
                commandLine(args)
            }

            if (result.exitValue == 1) {
                isForbidden = true
            }
        }

        if (isForbidden && failOnForbidden.get()) {
            throw GradleException("Trivy found forbidden or restricted licenses in dependencies!")
        }
    }
}
