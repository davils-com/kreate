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
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveRustTargets
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException

/**
 * Task to generate C-interop definition files for native targets.
 *
 * This task creates the necessary `.def` files used by Kotlin/Native to
 * interface with the compiled Rust libraries, including setting up
 * headers, static libraries, and library paths.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Definition files generation is fast and depends on external paths")
public abstract class GenerateDefinitionFiles : Task(
    "Generates cinterop definition files for native targets",
    "kreate c-interoperation"
) {
    /**
     * The working directory for the task.
     * @since 1.0.0
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val workDir: DirectoryProperty

    /**
     * The root directory of the project.
     * @since 1.0.0
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val rootDir: DirectoryProperty

    /**
     * The name of the project.
     * @since 1.0.0
     */
    @get:Input
    public abstract val projectName: Property<String>

    /**
     * The name of the definition file to generate.
     * @since 1.0.0
     */
    @get:Input
    public abstract val defFileName: Property<String>

    /**
     * The name of the directory to store definition files in.
     * @since 1.0.0
     */
    @get:Input
    public abstract val defDirName: Property<String>

    /**
     * The list of Rust targets to include in the definition file.
     * @since 1.0.0
     */
    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    /**
     * The output directory where definition files are generated.
     * @since 1.0.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(defDirName.get())

    /**
     * Executes the task to generate definition files.
     *
     * @since 1.0.0
     */
    @TaskAction
    override fun execute() {
        val cinterop = validateCInteropDir()
        val definition = validateDefinitionFile(cinterop)
        writeFileContent(definition)
    }

    private fun validateCInteropDir(): File {
        val dir = workDir.get().asFile.resolve(defDirName.get())
        if (!dir.exists() && !dir.mkdirs()) {
            throw GradleException("Failed to create cinterop directory: ${dir.absolutePath}")
        }
        return dir
    }

    private fun validateDefinitionFile(cinteropDir: File): File {
        val defFile = cinteropDir.resolve(defFileName.get())
        if (!defFile.exists()) {
            try {
                defFile.createNewFile()
            } catch (e: IOException) {
                throw GradleException("Failed to create cinterop definition file: ${defFile.absolutePath}", e)
            }
        }
        return defFile
    }

    private fun writeFileContent(defFile: File) {
        val rootDir = rootDir.get()
        val name = projectName.get()
        val targets = resolveRustTargets(rustTargets)
        val libraryPaths = targets.joinToString(" ") { target ->
            "${rootDir.asFile.name}/${name}/target/${target}/release"
        }

        defFile.writeText(
            """
                headers = $name.h
                staticLibraries = lib$name.a
                compilerOpts = -I${rootDir.asFile.name}/${name}/include
                libraryPaths = $libraryPaths
            """.trimIndent()
        )
    }
}
