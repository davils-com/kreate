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
package com.davils.kreate.githook.task

import com.davils.kreate.githook.hook.HookInstaller
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.vcs.GitHistory
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

private const val HOOK: String = "hooks/pre-commit"

@UntrackedTask(because = "It writes into the Git directory")
internal abstract class GitHookInstall @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Installs the pre-commit hook, which runs ./gradlew kreateGitHookRun.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun install() {
        val hook = hookFile(exec, projectDirectory.get().asFile)
        val outcome = try {
            HookInstaller.install(hook)
        } catch (cause: IllegalStateException) {
            throw GradleException(cause.message.orEmpty(), cause)
        }
        logger.lifecycle("${outcome.name.lowercase().replaceFirstChar(Char::uppercase)} ${hook.path}.")
    }
}

internal fun hookFile(exec: ExecOperations, directory: File): File {
    val git = GitHistory(ExecOperationsToolRunner(exec), ExecutableResolver.resolve(ExternalTool.GIT), directory)
    return git.gitPath(HOOK)
}
