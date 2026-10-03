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
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

private val DEFAULT_DEPENDENCIES: Map<String, String> = mapOf("libc" to "")

private val DEFAULT_BUILD_DEPENDENCIES: Map<String, String> = mapOf("cbindgen" to "")

@DisableCachingByDefault(because = "Rust dependency management has side effects on Cargo.toml")
internal abstract class AddRustDependencies @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Adds rust dependencies to the project.", KreateTaskGroup.C_INTEROP) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val rustDependencies: MapProperty<String, String>

    @get:Input
    @get:Optional
    public abstract val rustBuildDependencies: MapProperty<String, String>

    @TaskAction
    public fun execute() {
        val cargoToml = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)
        if (!cargoToml.exists()) {
            throw GradleException("Cargo.toml not found in ${workDir.get().asFile.absolutePath}")
        }
        val cargoContent = cargoToml.readText()
        val cargoCommand = ExecutableResolver.resolve(ExternalTool.CARGO)

        val dependencies = rustDependencies.orNull?.takeIf { it.isNotEmpty() } ?: DEFAULT_DEPENDENCIES
        val buildDependencies = rustBuildDependencies.orNull?.takeIf { it.isNotEmpty() } ?: DEFAULT_BUILD_DEPENDENCIES

        addMissingDependencies(cargoCommand, cargoContent, dependencies, isBuildDependency = false)
        addMissingDependencies(cargoCommand, cargoContent, buildDependencies, isBuildDependency = true)
    }

    private fun addMissingDependencies(
        cargoCommand: String,
        cargoContent: String,
        dependencies: Map<String, String>,
        isBuildDependency: Boolean
    ) {
        val missing = dependencies.filterKeys { name -> !cargoContent.contains(name) }
        missing.forEach { (name, version) -> addDependency(cargoCommand, name, version, isBuildDependency) }
    }

    private fun addDependency(cargoCommand: String, name: String, version: String, isBuildDependency: Boolean) {
        val command = buildList {
            add(cargoCommand)
            add("add")
            if (isBuildDependency) add("--build")
            add(dependencySpecification(name, version))
        }

        try {
            exec.exec {
                workingDir = workDir.get().asFile
                commandLine(command)
            }
        } catch (exception: GradleException) {
            throw GradleException("Failed to add rust dependency '$name'.", exception)
        }
    }

    private fun dependencySpecification(name: String, version: String): String {
        if (version.isEmpty()) return name
        return "$name@$version"
    }
}
