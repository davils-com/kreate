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

package com.davils.kreate.trivy.task

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivySecretScan @Inject constructor(
    exec: ExecOperations
) : TrivyScan(
    "Scans source files for secrets using Trivy",
    "secret",
    exec
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val secretConfig: RegularFileProperty

    @TaskAction
    public fun execute() {
        val withSecrets = sourceFiles.files.filter { file -> scanReportsFindings(file) }
        if (withSecrets.isEmpty() || !failOnFindings.get()) return

        val named = withSecrets.map { file -> file.invariantSeparatorsPath }.sorted()
        throw GradleException(
            "Trivy found secrets in ${named.size} source file(s):\n" +
                named.joinToString("\n") { "  $it" } +
                "\nEach file's findings are printed with its summary table above."
        )
    }

    override fun scannerArguments(): List<String> {
        val secretConfigPath = secretConfig.get().asFile.absolutePath
        return listOf("--secret-config", secretConfigPath)
    }
}
