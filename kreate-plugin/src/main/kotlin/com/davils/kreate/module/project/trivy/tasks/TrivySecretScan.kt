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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Scans source files for secrets using Trivy.
 *
 * This task uses the Trivy CLI to scan project source files for hardcoded secrets.
 * It can be configured to fail the build if any secrets are discovered.
 *
 * @param exec The [ExecOperations] used to run the Trivy command.
 * @since 1.2.0
 */
@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
public abstract class TrivySecretScan @Inject constructor(
    private val exec: ExecOperations
) : Task(
    "Scans source files for secrets using Trivy",
    "kreate trivy"
) {
    /**
     * The collection of source files to scan for secrets.
     *
     * @since 1.2.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection

    /**
     * The severity levels to report (e.g., HIGH, CRITICAL).
     *
     * @since 1.2.0
     */
    @get:Input
    public abstract val severity: ListProperty<String>

    /**
     * Whether the task should fail the build if any secrets are found.
     *
     * @since 1.2.0
     */
    @get:Input
    public abstract val failOnFindings: Property<Boolean>

    /**
     * The configuration file for Trivy secret scanning.
     *
     * @since 1.2.0
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val secretConfig: RegularFileProperty

    /**
     * Executes the secret scan by running Trivy on each source file.
     *
     * Runs the Trivy CLI in filesystem mode specifically for secret scanning.
     * Throws a [GradleException] if secrets are found and [failOnFindings] is true.
     *
     * @since 1.2.0
     */
    @TaskAction
    override fun execute() {
        var hasSecrets = false

        sourceFiles.forEach { file ->
            val result = exec.exec {
                isIgnoreExitValue = true
                commandLine(
                    resolveTrivyCommand(), "fs",
                    "--scanners", "secret",
                    "--secret-config", secretConfig.get().asFile.absolutePath,
                    "--severity", severity.get().joinToString(","),
                    "--exit-code", if (failOnFindings.get()) "1" else "0",
                    "--format", "table",
                    file.absolutePath
                )
            }
            if (result.exitValue == 1) {
                hasSecrets = true
            }
        }

        if (hasSecrets && failOnFindings.get()) {
            throw GradleException("Trivy found secrets in source files!")
        }
    }
}
