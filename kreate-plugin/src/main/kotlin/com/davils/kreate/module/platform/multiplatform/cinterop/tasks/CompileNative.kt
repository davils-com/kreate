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
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Task to compile a native C or C++ C-interop project using CMake.
 *
 * This task configures the CMake build inside a dedicated `build` directory
 * under the native project and then runs `cmake --build` to produce the static
 * library (`lib<projectName>.a`). The produced directory is exposed via
 * [outputDir] and is referenced by the generated definition file through its
 * `libraryPaths` entry.
 *
 * @param exec The executive operations used to run external commands.
 * @since 1.3.0
 */
@DisableCachingByDefault(because = "CMake build depends on external environment and tools")
public abstract class CompileNative @Inject constructor(
    /**
     * The executive operations instance.
     * @since 1.3.0
     */
    private val exec: ExecOperations
) : Task("Compile C/C++ code for C interop", "kreate c-interoperation") {
    /**
     * The native project directory (`<root>/<projectName>`).
     * @since 1.3.0
     */
    @get:Internal
    public abstract val workDir: DirectoryProperty

    /**
     * The CMake build type. Defaults to `Release`.
     * @since 1.3.0
     */
    @get:Input
    @get:Optional
    public abstract val buildType: Property<String>

    /**
     * The output directory containing the compiled native artifacts.
     * @since 1.3.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(BUILD_DIR_NAME)

    /**
     * Configures and builds the static library.
     *
     * @return Unit
     * @throws GradleException If the CMake invocation fails.
     * @since 1.3.0
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
        } catch (_: Exception) {
            throw GradleException("Failed to compile native C/C++ library.")
        }
    }

    /**
     * Companion object for [CompileNative].
     *
     * @since 1.3.0
     */
    public companion object {
        /**
         * The CMake build directory name inside the native project.
         * @since 1.3.0
         */
        public const val BUILD_DIR_NAME: String = "build"
    }
}
