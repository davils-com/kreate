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
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

private const val CMAKE_LIST_SEPARATOR: String = ";"

private val CMAKE_CONFIGURATIONS: List<String> = listOf("DEBUG", "RELEASE", "RELWITHDEBINFO", "MINSIZEREL")

@DisableCachingByDefault(because = "CMake configuration is tied to absolute paths and the local toolchain")
internal abstract class ConfigureNative @Inject constructor(
    private val exec: ExecOperations
) : KreateTask("Runs the CMake configure step for the native JNI project.", KreateTaskGroup.JNI) {
    @get:Internal
    public abstract val sourceDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val cmakeListsFile: RegularFileProperty

    @get:Input
    public abstract val cacheBoundPaths: ListProperty<String>

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val generatedHeaderDirectory: DirectoryProperty

    @get:Input
    public abstract val buildType: Property<String>

    @get:Input
    @get:Optional
    public abstract val generator: Property<String>

    @get:Input
    @get:Optional
    public abstract val cmakeExecutable: Property<String>

    @get:Input
    public abstract val javaHome: Property<String>

    @get:Input
    public abstract val libraryIncludePaths: ListProperty<String>

    @get:Internal
    public abstract val cmakeBuildDirectory: DirectoryProperty

    @get:Internal
    public abstract val libraryOutputDirectory: DirectoryProperty

    @get:OutputFile
    public abstract val cmakeCache: RegularFileProperty

    @TaskAction
    public fun execute() {
        val buildDirectory = cmakeBuildDirectory.get().asFile
        val libraryDirectory = libraryOutputDirectory.get().asFile
        buildDirectory.mkdirs()
        libraryDirectory.mkdirs()

        val libraryPath = libraryDirectory.toCmakePath()
        val javaHomePath = javaHome.get().toCmakePath()
        val cmakeCommand = ExecutableResolver.resolve(ExternalTool.CMAKE, cmakeExecutable.orNull)

        val arguments = buildList {
            add(cmakeCommand)
            add("-S")
            add(sourceDirectory.get().asFile.toCmakePath())
            add("-B")
            add(buildDirectory.toCmakePath())
            addAll(generatorArguments())
            add("-DCMAKE_BUILD_TYPE=${buildType.get()}")
            add("-DJAVA_HOME=$javaHomePath")
            addAll(outputDirectoryArguments(libraryPath))
            addAll(includeDirectoryArguments())
        }

        runCmake(
            exec = exec,
            logger = logger,
            phase = "configure",
            workingDirectory = sourceDirectory.get().asFile,
            javaHome = javaHomePath,
            arguments = arguments
        )

        requireCmakeCache()
    }

    private fun generatorArguments(): List<String> {
        val selectedGenerator = generator.orNull?.takeIf { it.isNotBlank() } ?: return emptyList()
        return listOf("-G", selectedGenerator)
    }

    private fun outputDirectoryArguments(libraryPath: String): List<String> {
        val configurationSuffixes = listOf("") + CMAKE_CONFIGURATIONS.map { configuration -> "_$configuration" }
        return configurationSuffixes.flatMap { suffix ->
            listOf(
                "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY$suffix=$libraryPath",
                "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY$suffix=$libraryPath"
            )
        }
    }

    private fun includeDirectoryArguments(): List<String> {
        val generatedHeaders = generatedHeaderDirectory.orNull?.asFile?.toCmakePath()
        val libraryIncludes = libraryIncludePaths.getOrElse(emptyList()).filter { it.isNotBlank() }
        val includePaths = listOfNotNull(generatedHeaders) + libraryIncludes.map { it.toCmakePath() }
        if (includePaths.isEmpty()) return emptyList()

        return listOf("-D$JNI_INCLUDE_DIRS_VARIABLE=${includePaths.joinToString(CMAKE_LIST_SEPARATOR)}")
    }

    private fun requireCmakeCache() {
        val cache = cmakeCache.get().asFile
        if (cache.isFile) return

        throw GradleException(
            "CMake reported success but did not write ${cache.absolutePath}. " +
                "This usually means the configured generator wrote to a different directory."
        )
    }
}
