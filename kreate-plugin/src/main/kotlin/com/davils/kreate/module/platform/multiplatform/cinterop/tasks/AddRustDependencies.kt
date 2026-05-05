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
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Task to add Rust dependencies to the `Cargo.toml` file.
 *
 * This task uses the `cargo add` command to register dependencies and build
 * dependencies for the Rust project, ensuring that required crates like
 * `libc` and `cbindgen` are available.
 *
 * @param exec The executive operations used to run external commands.
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Rust dependency management has side effects on Cargo.toml")
public abstract class AddRustDependencies @Inject constructor(
    /**
     * The executive operations instance.
     * @since 1.0.0
     */
    private val exec: ExecOperations
) : Task("Adds rust dependencies to the project.", "kreate c-interoperation") {
    /**
     * The working directory containing the Rust project.
     * @since 1.0.0
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val workDir: DirectoryProperty

    /**
     * Map of Rust dependencies to add (name to version).
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    public abstract val rustDependencies: MapProperty<String, String>

    /**
     * Map of Rust build dependencies to add (name to version).
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    public abstract val rustBuildDependencies: MapProperty<String, String>

    /**
     * Executes the task to add dependencies to `Cargo.toml`.
     *
     * @throws GradleException If `Cargo.toml` is missing or `cargo add` fails.
     * @since 1.0.0
     */
    @TaskAction
    override fun execute() {
        val cargoToml = workDir.get().asFile.resolve("Cargo.toml")
        if (!cargoToml.exists()) {
            throw GradleException("Cargo.toml not found in ${workDir.get().asFile.absolutePath}")
        }
        val cargoContent = cargoToml.readText()

        val deps = if (rustDependencies.isPresent && rustDependencies.get().isNotEmpty()) {
            rustDependencies.get()
        } else {
            mapOf("libc" to "")
        }

        val buildDeps = if (rustBuildDependencies.isPresent && rustBuildDependencies.get().isNotEmpty()) {
            rustBuildDependencies.get()
        } else {
            mapOf("cbindgen" to "")
        }

        val cargoCmd = resolveCargoCommand()

        for ((name, version) in deps) {
            if (isDependencyPresent(cargoContent, name)) continue
            addDependency(cargoCmd = cargoCmd, name = name, version = version, build = false)
        }

        for ((name, version) in buildDeps) {
            if (isDependencyPresent(cargoContent, name)) continue
            addDependency(cargoCmd = cargoCmd, name = name, version = version, build = true)
        }
    }

    private fun isDependencyPresent(cargoContent: String, name: String): Boolean = cargoContent.contains(name)

    private fun addDependency(cargoCmd: String, name: String, version: String, build: Boolean) {
        try {
            exec.exec {
                workingDir = workDir.get().asFile
                val cmd = mutableListOf(cargoCmd, "add")
                if (build) cmd.add("--build")
                if (version.isNotEmpty()) cmd.add("$name@$version") else cmd.add(name)
                commandLine(cmd)
            }
        } catch (_: Exception) {
            throw GradleException("Failed to add rust dependency '$name'.")
        }
    }
}
