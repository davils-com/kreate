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

package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveCargoCommand
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveRustTargets
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class CompileRust @Inject constructor(
    private val exec: ExecOperations
) : Task("Compile Rust code for C interop") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve("target")

    @TaskAction
    override fun execute() {
        val targets = resolveRustTargets(rustTargets)
        val cargoCmd = resolveCargoCommand()

        for (target in targets) {
            try {
                exec.exec {
                    workingDir = workDir.get().asFile
                    commandLine(cargoCmd, "build", "--target", target, "--release")
                }
            } catch (e: Exception) {
                throw GradleException("Failed to compile Rust code for target '$target'.", e)
            }
        }
    }
}
