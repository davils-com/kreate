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
import com.davils.kreate.module.platform.multiplatform.cinterop.NativeLanguage
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Task to initialize a new native C or C++ C-interop project.
 *
 * This task scaffolds a CMake-based project under `<workDir>/<projectName>`
 * containing a `CMakeLists.txt`, an `include/<projectName>.h` public header,
 * and a placeholder implementation in `src/<projectName>.c` (for C) or
 * `src/<projectName>.cpp` (for C++). The generated CMake configuration builds a
 * static library named `<projectName>` consumable by Kotlin/Native.
 *
 * Existing files are never overwritten, so the task is safe to re-run and can
 * be pointed at an already existing native project.
 *
 * @since 1.3.0
 */
@DisableCachingByDefault(because = "Initialization task is only intended to run once and has conditional side effects")
public abstract class InitializeNativeProject : Task(
    "Generates a new native C/C++ C-interop project.",
    "kreate c-interoperation"
) {
    /**
     * The root working directory where the native project will be created.
     * @since 1.3.0
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val workDir: DirectoryProperty

    /**
     * The name of the native project. Used as the CMake `project(...)` name,
     * the produced static library target name, and the public header name.
     *
     * @since 1.3.0
     */
    @get:Input
    public abstract val projectName: Property<String>

    /**
     * The native source language to scaffold for.
     *
     * Only [NativeLanguage.C] and [NativeLanguage.CPP] are valid here; the Rust
     * pipeline uses a dedicated set of tasks.
     *
     * @since 1.3.0
     */
    @get:Input
    public abstract val language: Property<NativeLanguage>

    /**
     * The output directory that represents the native project root.
     *
     * @since 1.3.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(projectName.get())

    /**
     * Executes the scaffolding, creating the project structure and the initial
     * sources when they are missing.
     *
     * @return Unit
     * @throws GradleException If a required directory cannot be created.
     * @since 1.3.0
     */
    @TaskAction
    override fun execute() {
        val name = projectName.get()
        val lang = language.get()
        val projectRoot = workDir.get().asFile.resolve(name)

        val srcDir = projectRoot.resolve(SRC_DIR_NAME)
        if (!srcDir.exists() && !srcDir.mkdirs()) {
            throw GradleException("Failed to create src directory: ${srcDir.absolutePath}")
        }

        val includeDir = projectRoot.resolve(INCLUDE_DIR_NAME)
        if (!includeDir.exists() && !includeDir.mkdirs()) {
            throw GradleException("Failed to create include directory: ${includeDir.absolutePath}")
        }

        val cMake = projectRoot.resolve(CMAKE_FILE_NAME)
        if (!cMake.exists()) {
            cMake.writeText(cMakeContent(name, lang))
        }

        val header = includeDir.resolve("$name.h")
        if (!header.exists()) {
            header.writeText(headerContent(name))
        }

        val sourceExtension = if (lang == NativeLanguage.CPP) "cpp" else "c"
        val source = srcDir.resolve("$name.$sourceExtension")
        if (!source.exists()) {
            source.writeText(sourceContent(name))
        }
    }

    private fun cMakeContent(name: String, language: NativeLanguage): String {
        val (langId, standardBlock, glob) = when (language) {
            NativeLanguage.CPP -> Triple(
                "CXX",
                "set(CMAKE_CXX_STANDARD 17)\nset(CMAKE_CXX_STANDARD_REQUIRED ON)",
                "\"src/*.cpp\" \"src/*.cc\""
            )
            else -> Triple(
                "C",
                "set(CMAKE_C_STANDARD 11)\nset(CMAKE_C_STANDARD_REQUIRED ON)",
                "\"src/*.c\""
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

    /**
     * Companion object for [InitializeNativeProject].
     *
     * @since 1.3.0
     */
    public companion object {
        /**
         * The CMake file name.
         * @since 1.3.0
         */
        public const val CMAKE_FILE_NAME: String = "CMakeLists.txt"

        /**
         * The source directory name inside the native project.
         * @since 1.3.0
         */
        public const val SRC_DIR_NAME: String = "src"

        /**
         * The public header directory name inside the native project.
         * @since 1.3.0
         */
        public const val INCLUDE_DIR_NAME: String = "include"
    }
}
