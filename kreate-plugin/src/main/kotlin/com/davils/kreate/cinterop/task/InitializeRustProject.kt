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

package com.davils.kreate.cinterop.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Initialization task is only intended to run once and has conditional side effects")
internal abstract class InitializeRustProject @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Generates a new rust project.", KreateTaskGroup.C_INTEROP) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(projectName.get())

    @TaskAction
    public fun execute() {
        val workDirFile = workDir.get().asFile
        val name = projectName.get()
        val rustProjectDir = workDirFile.resolve(name)
        if (rustProjectDir.exists()) return

        val cargoCommand = ExecutableResolver.resolve(ExternalTool.CARGO)
        try {
            exec.exec {
                workingDir = workDirFile
                commandLine(cargoCommand, "new", "--lib", name)
            }
        } catch (exception: GradleException) {
            throw GradleException("Failed to initialize rust project.", exception)
        }
    }
}
