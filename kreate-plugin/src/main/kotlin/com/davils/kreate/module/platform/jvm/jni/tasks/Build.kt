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

package com.davils.kreate.module.platform.jvm.jni.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.jvm.jni.resolveCmakeCommand
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Task to build the native JNI library using CMake.
 *
 * This task runs `cmake` to configure the build inside a dedicated `build`
 * directory under the native project, then executes `cmake --build` to produce
 * the shared library (`.dylib`/`.so`/`.dll`). The produced binary directory is
 * exposed via [outputDir] and is intended to be added to `java.library.path`.
 *
 * @param exec The executive operations used to run external commands.
 * @since 1.1.0
 */
public abstract class BuildNative @Inject constructor(
    /**
     * The executive operations instance.
     * @since 1.1.0
     */
    private val exec: ExecOperations
) : Task("Builds the native JNI library with CMake.") {
    /**
     * The native project directory (`<root>/<projectName>`).
     * @since 1.1.0
     */
    @get:Internal
    public abstract val workDir: DirectoryProperty

    /**
     * The CMake build type. Defaults to `Release`.
     * @since 1.1.0
     */
    @get:Input
    @get:Optional
    public abstract val buildType: Property<String>

    /**
     * The output directory containing the compiled native artifacts.
     * @since 1.1.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve("build")

    /**
     * Configures and builds the native library.
     *
     * @return Unit
     * @throws GradleException If the CMake invocation fails.
     * @since 1.1.0
     */
    @TaskAction
    override fun execute() {
        val projectRoot = workDir.get().asFile
        val buildDir = outputDir
        if (!buildDir.exists() && !buildDir.mkdirs()) {
            throw GradleException("Failed to create CMake build directory: ${buildDir.absolutePath}")
        }

        val type = buildType.orNull ?: "Release"
        val cmakeCmd = resolveCmakeCommand()

        try {
            exec.exec {
                workingDir = projectRoot
                commandLine(cmakeCmd, "-S", ".", "-B", buildDir.absolutePath, "-DCMAKE_BUILD_TYPE=$type")
            }
            exec.exec {
                workingDir = projectRoot
                commandLine(cmakeCmd, "--build", buildDir.absolutePath, "--config", type)
            }
        } catch (e: Exception) {
            throw GradleException("Failed to build native JNI library.", e)
        }
    }
}
