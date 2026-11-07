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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

@UntrackedTask(because = "It writes into the Git directory")
internal abstract class GitHookUninstall @Inject constructor(private val exec: ExecOperations) : KreateTask(
    "Removes the pre-commit hook, if Kreate installed it.",
    KreateTaskGroup.KREATE
) {
    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun uninstall() {
        val hook = hookFile(exec, projectDirectory.get().asFile)
        if (HookInstaller.uninstall(hook)) return logger.lifecycle("Removed ${hook.path}.")
        logger.lifecycle("There is no pre-commit hook from Kreate at ${hook.path}.")
    }
}
