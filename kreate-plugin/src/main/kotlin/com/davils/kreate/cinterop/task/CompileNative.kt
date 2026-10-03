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
import com.davils.kreate.tooling.runExternalTool
import com.davils.kreate.tooling.toCmakePath
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

private const val BUILD_DIR_NAME: String = "build"

private const val DEFAULT_BUILD_TYPE: String = "Release"

@DisableCachingByDefault(because = "CMake build depends on external environment and tools")
internal abstract class CompileNative @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Compile C/C++ code for C interop", KreateTaskGroup.C_INTEROP) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val nativeSources: ConfigurableFileCollection

    @get:Input
    @get:Optional
    public abstract val buildType: Property<String>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(BUILD_DIR_NAME)

    @TaskAction
    public fun execute() {
        val projectRoot = workDir.get().asFile
        val buildDir = outputDir
        if (!buildDir.exists() && !buildDir.mkdirs()) {
            throw GradleException("Failed to create CMake build directory: ${buildDir.absolutePath}")
        }

        val type = buildType.orNull ?: DEFAULT_BUILD_TYPE
        val cmakeCmd = ExecutableResolver.resolve(ExternalTool.CMAKE)

        runExternalTool(
            exec = exec,
            logger = logger,
            description = "CMake configure",
            workingDirectory = projectRoot,
            arguments = listOf(cmakeCmd, "-S", ".", "-B", buildDir.toCmakePath(), "-DCMAKE_BUILD_TYPE=$type")
        )
        runExternalTool(
            exec = exec,
            logger = logger,
            description = "CMake build",
            workingDirectory = projectRoot,
            arguments = listOf(cmakeCmd, "--build", buildDir.toCmakePath(), "--config", type)
        )
    }
}
