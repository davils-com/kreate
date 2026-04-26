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
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Task to initialize a new Rust project.
 *
 * This task runs the `cargo new --lib` command to create a basic Rust library
 * project if it doesn't already exist in the specified working directory.
 *
 * @param exec The executive operations used to run external commands.
 * @since 1.0.0
 */
public abstract class InitializeRustProject @Inject constructor(
    /**
     * The executive operations instance.
     * @since 1.0.0
     */
    private val exec: ExecOperations
) : Task("Generates a new rust project.", "kreate c-interoperation") {
    /**
     * The working directory where the Rust project will be created.
     * @since 1.0.0
     */
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    /**
     * The name of the Rust project to create.
     * @since 1.0.0
     */
    @get:Input
    public abstract val projectName: Property<String>

    /**
     * The output directory where the Rust project is initialized.
     * @since 1.0.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(projectName.get())

    /**
     * Executes the task to initialize the Rust project.
     *
     * @throws GradleException If the project initialization fails.
     * @since 1.0.0
     */
    @TaskAction
    override fun execute() {
        val workDirFile = workDir.get().asFile
        val projectName = projectName.get()
        val rustProjectDir = workDirFile.resolve(projectName)

        if (rustProjectDir.exists()) {
            return
        }

        val cargoCmd = resolveCargoCommand()
        try {
            exec.exec {
                workingDir = workDirFile
                commandLine(cargoCmd, "new", "--lib", projectName)
            }
        } catch (e: Exception) {
            throw GradleException("Failed to initialize rust project.", e)
        }
    }
}
