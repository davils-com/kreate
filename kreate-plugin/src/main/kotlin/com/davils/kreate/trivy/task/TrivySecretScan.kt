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

import com.davils.kreate.assurance.finding.Finding
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

private const val STAGE_DIRECTORY: String = "sources"

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

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val secretConfig: ConfigurableFileCollection

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun execute() {
        val staged = temporaryDir.resolve(STAGE_DIRECTORY)
        staged.deleteRecursively()
        sourceFiles.files.forEach { file -> stage(file, staged) }

        val run = runTrivy(TrivyTarget(staged) { path -> path.removePrefix(staged.invariantSeparatorsPath + "/") })
        publish(listOf(run), ::findingsMessage)
    }

    private fun stage(file: File, staged: File) {
        val root = projectDirectory.get().asFile
        val copy = staged.resolve(file.relativeTo(root).path)
        copy.parentFile.mkdirs()
        file.copyTo(copy, overwrite = true)
    }

    private fun findingsMessage(findings: List<Finding>): String {
        val paths = findings.mapNotNull { finding -> finding.location?.path }
        val named = paths.distinct().sorted()
        return "Trivy found secrets in ${named.size} source file(s):\n" +
            named.joinToString("\n") { "  $it" } +
            "\nThe findings are listed above."
    }

    override fun scannerArguments(): List<String> {
        val configuration = secretConfig.files.firstOrNull { file -> file.isFile } ?: return emptyList()
        return listOf("--secret-config", configuration.absolutePath)
    }
}
