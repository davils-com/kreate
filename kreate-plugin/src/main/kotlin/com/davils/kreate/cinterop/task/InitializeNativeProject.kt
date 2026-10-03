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
import com.davils.kreate.task.CMAKE_LISTS_FILE_NAME
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

private const val SRC_DIR_NAME: String = "src"

private const val INCLUDE_DIR_NAME: String = "include"

@DisableCachingByDefault(because = "Initialization task is only intended to run once and has conditional side effects")
internal abstract class InitializeNativeProject : KreateTask(
    "Generates a new native C/C++ C-interop project.",
    KreateTaskGroup.C_INTEROP
) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

    @get:Input
    public abstract val language: Property<NativeLanguage>

    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(projectName.get())

    @TaskAction
    public fun execute() {
        val name = projectName.get()
        val nativeLanguage = language.get()
        val projectRoot = workDir.get().asFile.resolve(name)

        val srcDir = projectRoot.resolve(SRC_DIR_NAME)
        ensureDirectory(srcDir, SRC_DIR_NAME)
        val includeDir = projectRoot.resolve(INCLUDE_DIR_NAME)
        ensureDirectory(includeDir, INCLUDE_DIR_NAME)

        writeIfAbsent(projectRoot.resolve(CMAKE_LISTS_FILE_NAME)) { cMakeContent(name, nativeLanguage) }
        writeIfAbsent(includeDir.resolve("$name.h")) { headerContent(name) }
        writeIfAbsent(srcDir.resolve("$name.${sourceExtension(nativeLanguage)}")) { sourceContent(name) }
    }

    private fun ensureDirectory(directory: File, description: String) {
        if (directory.exists() || directory.mkdirs()) return
        throw GradleException("Failed to create $description directory: ${directory.absolutePath}")
    }

    private fun writeIfAbsent(file: File, content: () -> String) {
        if (file.exists()) return
        file.writeText(content())
    }

    private fun sourceExtension(nativeLanguage: NativeLanguage): String {
        if (nativeLanguage == NativeLanguage.CPP) return "cpp"
        return "c"
    }

    private fun cMakeContent(name: String, language: NativeLanguage): String {
        val (langId, standardBlock, glob) = when (language) {
            NativeLanguage.CPP -> Triple(
                "CXX",
                """
                    set(CMAKE_CXX_STANDARD 17)
                    set(CMAKE_CXX_STANDARD_REQUIRED ON)
                """.trimIndent(),
                "\"src/*.cpp\" \"src/*.cc\""
            )
            NativeLanguage.C -> Triple(
                "C",
                """
                    set(CMAKE_C_STANDARD 11)
                    set(CMAKE_C_STANDARD_REQUIRED ON)
                """.trimIndent(),
                "\"src/*.c\""
            )
            NativeLanguage.RUST -> error(
                "Rust C-interop projects are scaffolded by InitializeRustProject, not by this task."
            )
        }
        return """
            cmake_minimum_required(VERSION 3.20)
            project($name $langId)
            $standardBlock
            set(CMAKE_POSITION_INDEPENDENT_CODE ON)

            file(GLOB ${name.uppercase()}_SOURCES $glob)

            add_library($name STATIC ${'$'}{${name.uppercase()}_SOURCES})
            target_include_directories($name PUBLIC include)
        """.trimIndent()
    }

    private fun headerContent(name: String): String {
        val guard = "${name.uppercase()}_H"
        return """
            #ifndef $guard
            #define $guard

            #ifdef __cplusplus
            extern "C" {
            #endif

            // Public C-interop API for "$name".
            // Declare the functions exposed to Kotlin/Native here.
            int ${name}_hello(void);

            #ifdef __cplusplus
            }
            #endif

            #endif // $guard
        """.trimIndent()
    }

    private fun sourceContent(name: String): String = """
        #include "$name.h"

        // Placeholder implementation for C-interop project "$name".
        int ${name}_hello(void) {
            return 0;
        }
    """.trimIndent()
}
