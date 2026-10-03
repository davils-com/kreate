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

import com.davils.kreate.task.CMAKE_LISTS_FILE_NAME
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

internal const val JNI_INCLUDE_DIRS_VARIABLE: String = "KREATE_JNI_INCLUDE_DIRS"

private const val SRC_DIR_NAME: String = "src"

@DisableCachingByDefault(because = "Scaffolding runs once and deliberately preserves existing files")
internal abstract class InitializeCppProject : KreateTask(
    "Generates a new native C++ JNI project.",
    KreateTaskGroup.JNI
) {
    @get:Input
    public abstract val projectName: Property<String>

    @get:OutputDirectory
    public abstract val projectRoot: DirectoryProperty

    @TaskAction
    public fun execute() {
        val name = projectName.get()
        val root = projectRoot.get().asFile
        val sourceDirectory = root.resolve(SRC_DIR_NAME)

        if (!sourceDirectory.isDirectory && !sourceDirectory.mkdirs()) {
            throw GradleException("Failed to create the JNI source directory: ${sourceDirectory.absolutePath}")
        }

        scaffoldCMakeLists(root.resolve(CMAKE_LISTS_FILE_NAME), name)
        scaffoldPlaceholderSource(sourceDirectory.resolve("$name.cpp"), name)
    }

    private fun scaffoldCMakeLists(cMakeFile: File, name: String) {
        if (cMakeFile.exists()) {
            warnOnMissingIncludes(cMakeFile)
            return
        }

        cMakeFile.writeText(defaultCMakeContent(name))
        logger.lifecycle("Created ${cMakeFile.absolutePath}.")
    }

    private fun warnOnMissingIncludes(cMakeFile: File) {
        val referencesIncludeDirectories = cMakeFile.readText().contains(JNI_INCLUDE_DIRS_VARIABLE)
        if (referencesIncludeDirectories) return

        logger.warn(
            "${cMakeFile.absolutePath} does not reference \${$JNI_INCLUDE_DIRS_VARIABLE}. " +
                "Add it to target_include_directories(...) so that generated JNI headers and " +
                "the configured libraryIncludePaths are visible to the compiler."
        )
    }

    private fun scaffoldPlaceholderSource(placeholder: File, name: String) {
        if (placeholder.exists()) return

        placeholder.writeText(defaultSourceContent(name))
        logger.lifecycle("Created ${placeholder.absolutePath}.")
    }

    private fun defaultCMakeContent(projectName: String): String {
        val sourcesVariable = "${projectName.uppercase()}_SOURCES"
        return """
            cmake_minimum_required(VERSION 3.20)
            project($projectName CXX)
            set(CMAKE_CXX_STANDARD 17)
            set(CMAKE_CXX_STANDARD_REQUIRED ON)
            set(CMAKE_POSITION_INDEPENDENT_CODE ON)

            find_package(JNI REQUIRED)

            # CONFIGURE_DEPENDS re-evaluates the glob on every build system invocation, so a
            # newly added source file is picked up without a manual reconfigure.
            file(GLOB $sourcesVariable CONFIGURE_DEPENDS "src/*.cpp" "src/*.cc")

            add_library($projectName SHARED ${'$'}{$sourcesVariable})
            target_include_directories($projectName PRIVATE
                ${'$'}{JNI_INCLUDE_DIRS}
                include
                ${'$'}{$JNI_INCLUDE_DIRS_VARIABLE}
            )
            target_link_libraries($projectName PRIVATE ${'$'}{JNI_LIBRARIES})
        """.trimIndent()
    }

    private fun defaultSourceContent(projectName: String): String = """
        #include <jni.h>

        // Placeholder source for JNI project "$projectName".
        //
        // Run `gradle kreateJniHeaders` to generate declarations for every `external`
        // function in this module, then include the generated header here and implement
        // the declared functions.
    """.trimIndent()
}
