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

package com.davils.kreate.jni.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecutableResolver
import com.davils.kreate.tooling.ExternalTool
import com.davils.kreate.tooling.toCmakePath
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

private const val DEFAULT_BUILD_TYPE: String = "Release"

@DisableCachingByDefault(because = "Native artifacts are tied to the local toolchain and are not relocatable")
internal abstract class BuildNative @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Builds the native JNI library with CMake.", KreateTaskGroup.JNI) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val nativeSources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract val cmakeCache: RegularFileProperty

    @get:Input
    @get:Optional
    public abstract val buildType: Property<String>

    @get:Input
    @get:Optional
    public abstract val cmakeExecutable: Property<String>

    @get:Input
    public abstract val javaHome: Property<String>

    @get:Internal
    public abstract val cmakeBuildDirectory: DirectoryProperty

    @get:OutputDirectory
    public abstract val libraryOutputDirectory: DirectoryProperty

    @TaskAction
    public fun execute() {
        val type = buildType.getOrElse(DEFAULT_BUILD_TYPE)
        val cmakeCommand = ExecutableResolver.resolve(ExternalTool.CMAKE, cmakeExecutable.orNull)

        runCmake(
            exec = exec,
            logger = logger,
            phase = "build",
            workingDirectory = cmakeBuildDirectory.get().asFile,
            javaHome = javaHome.get().toCmakePath(),
            arguments = listOf(
                cmakeCommand,
                "--build",
                cmakeBuildDirectory.get().asFile.toCmakePath(),
                "--config",
                type
            )
        )
    }
}
