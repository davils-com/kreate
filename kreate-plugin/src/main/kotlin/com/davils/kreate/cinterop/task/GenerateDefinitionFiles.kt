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

import com.davils.kreate.cinterop.NativeLanguage
import com.davils.kreate.cinterop.rust.resolveRustTargets
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException

@DisableCachingByDefault(because = "Definition files generation is fast and depends on external paths")
internal abstract class GenerateDefinitionFiles : KreateTask(
    "Generates cinterop definition files for native targets",
    KreateTaskGroup.C_INTEROP
) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Internal
    public abstract val rootDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

    @get:Input
    public abstract val defFileName: Property<String>

    @get:Input
    public abstract val defDirName: Property<String>

    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    @get:Input
    public abstract val language: Property<NativeLanguage>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(defDirName.get())

    @TaskAction
    public fun execute() {
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
        val rootDirectoryName = rootDir.get().asFile.name
        val name = projectName.get()
        val nativeProjectPath = "$rootDirectoryName/$name"
        val libraryPaths = when (language.get()) {
            NativeLanguage.RUST -> rustLibraryPaths(nativeProjectPath)
            NativeLanguage.C, NativeLanguage.CPP -> "$nativeProjectPath/build"
        }

        defFile.writeText(
            """
                headers = $name.h
                staticLibraries = lib$name.a
                compilerOpts = -I$nativeProjectPath/include
                libraryPaths = $libraryPaths
            """.trimIndent()
        )
    }

    private fun rustLibraryPaths(nativeProjectPath: String): String {
        val declaredTargets = rustTargets.getOrElse(emptyList())
        val targets = resolveRustTargets(declaredTargets)
        return targets.joinToString(" ") { target -> "$nativeProjectPath/target/$target/release" }
    }
}
