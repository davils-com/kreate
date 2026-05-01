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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to initialize a new native C++ JNI project.
 *
 * This task creates a `<workDir>/<projectName>/src` directory and generates a
 * minimal `CMakeLists.txt` at `<workDir>/<projectName>/CMakeLists.txt`. It also
 * ensures a placeholder source file exists so that CMake has something to build
 * on first invocation.
 *
 * @since 1.1.0
 */
public abstract class InitializeCppProject : Task("Generates a new native C++ JNI project.", "kreate jni") {
    /**
     * The root working directory where the native project will be created.
     * @since 1.1.0
     */
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    /**
     * The name of the native project. Used as the CMake `project(...)` name and
     * as the produced library target name.
     *
     * @since 1.1.0
     */
    @get:Input
    public abstract val projectName: Property<String>

    /**
     * The output directory that represents the native project root.
     *
     * @since 1.1.0
     */
    @get:OutputDirectory
    public val outputDir: File
        get() = workDir.get().asFile.resolve(projectName.get())

    /**
     * Executes the initialization: creates the project structure and the
     * initial `CMakeLists.txt` when missing.
     *
     * @return Unit
     * @since 1.1.0
     */
    @TaskAction
    override fun execute() {
        val projectName = projectName.get()
        val projectRoot = workDir.get().asFile.resolve(projectName)
        val srcDir = projectRoot.resolve(SRC_DIR_NAME)
        if (!srcDir.exists() && !srcDir.mkdirs()) {
            error("Failed to create src directory: ${srcDir.absolutePath}")
        }

        val cMake = projectRoot.resolve(CMAKE_FILE_NAME)
        if (!cMake.exists()) {
            cMake.writeText(defaultCMakeContent(projectName))
        }

        val placeholder = srcDir.resolve("$projectName.cpp")
        if (!placeholder.exists()) {
            placeholder.writeText(defaultSourceContent(projectName))
        }
    }

    /**
     * Generates the default content for the CMakeLists.txt file.
     *
     * @param projectName The name of the project.
     * @return The generated CMake configuration as a string.
     * @since 1.1.0
     */
    private fun defaultCMakeContent(projectName: String): String = $$"""
        cmake_minimum_required(VERSION 3.20)
        project($$projectName CXX)
        set(CMAKE_CXX_STANDARD 17)
        set(CMAKE_CXX_STANDARD_REQUIRED ON)
        set(CMAKE_POSITION_INDEPENDENT_CODE ON)

        find_package(JNI REQUIRED)

        file(GLOB $${projectName.uppercase()}_SOURCES "src/*.cpp" "src/*.cc")

        add_library($$projectName SHARED ${$${projectName.uppercase()}_SOURCES})
        target_include_directories($$projectName PRIVATE ${JNI_INCLUDE_DIRS} include)
        target_link_libraries($$projectName PRIVATE ${JNI_LIBRARIES})
    """.trimIndent()

    /**
     * Generates the default content for the initial C++ source file.
     *
     * @param projectName The name of the project.
     * @return The generated C++ source code as a string.
     * @since 1.1.0
     */
    private fun defaultSourceContent(projectName: String): String = """
        #include <jni.h>

        // Placeholder source for JNI project "$projectName".
        // Implement your native methods here.
    """.trimIndent()

    public companion object {
        /**
         * The CMake file name.
         * @since 1.1.0
         */
        public const val CMAKE_FILE_NAME: String = "CMakeLists.txt"

        /**
         * The source directory name inside the native project.
         * @since 1.1.0
         */
        public const val SRC_DIR_NAME: String = "src"
    }
}
