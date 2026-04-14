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

/**
 * Task to compile Rust code for C-interop.
 *
 * This task executes the `cargo build` command for each specified Rust target,
 * producing static libraries that can be used by Kotlin/Native.
 *
 * @param exec The executive operations used to run external commands.
 * @since 1.0.0
 */
public abstract class CompileRust @Inject constructor(
    /**
     * The executive operations instance.
     * @since 1.0.0
     */
    private val exec: ExecOperations
) : Task("Compile Rust code for C interop") {
    /**
     * The working directory containing the Rust project.
     * @since 1.0.0
     */
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    /**
     * The list of Rust targets to compile for.
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    /**
     * The output directory where compiled artifacts are stored.
     * @since 1.0.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve("target")

    /**
     * Executes the task to compile Rust code for the configured targets.
     *
     * @throws GradleException If compilation fails for any target.
     * @since 1.0.0
     */
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
