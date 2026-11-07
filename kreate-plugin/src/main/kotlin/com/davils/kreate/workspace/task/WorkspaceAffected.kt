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
package com.davils.kreate.workspace.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.vcs.GitHistory
import com.davils.kreate.workspace.affected.AffectedProjects
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "It compares the working tree with a revision")
internal abstract class WorkspaceAffected @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Lists the projects that changed since a revision, with every project depending on them.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Internal
    @get:Option(option = "base", description = "The revision to compare with.")
    public abstract val base: Property<String>

    @get:Internal
    public abstract val descriptors: ConfigurableFileCollection

    @get:Internal
    public abstract val rootDirectory: DirectoryProperty

    @get:Internal
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun affected() {
        val root = rootDirectory.get().asFile
        val git = GitHistory(ExecOperationsToolRunner(exec), ExecutableResolver.resolve(ExternalTool.GIT), root)
        val revision = base.get()
        val changed = try {
            git.changedFiles(revision)
        } catch (cause: IllegalArgumentException) {
            throw InvalidUserDataException(cause.message.orEmpty(), cause)
        }
        val prefix = prefixOf(git.root(), root)
        val relative = changed.filter { file -> file.startsWith(prefix) }.map { file -> file.removePrefix(prefix) }
        val affected = AffectedProjects.affectedBy(membersOf(descriptors), relative)
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(affected.joinToString("\n", postfix = "\n"))
        logger.lifecycle("${affected.size} project(s) affected since $revision:")
        affected.forEach { path -> logger.lifecycle("  $path") }
    }

    private fun prefixOf(gitRoot: File, root: File): String {
        val gitPath = gitRoot.toPath().toRealPath()
        val relative = gitPath.relativize(root.toPath().toRealPath()).joinToString("/")
        if (relative.isEmpty()) return ""
        return "$relative/"
    }
}
