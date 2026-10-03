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

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val PROBE_PROJECT = "cmake_minimum_required(VERSION 3.20)\nproject(probe NONE)\n"

private fun isOnPath(command: String): Boolean {
    val path = System.getenv("PATH").orEmpty()
    val directories = path.split(File.pathSeparatorChar)
    return directories.any { directory -> File(directory, command).canExecute() }
}

class ConfigureNativeTest : FunSpec({
    val workspace = tempdir()
    val isCmakeInstalled = isOnPath("cmake")
    val isMakeInstalled = isOnPath("make")

    fun configureTask(root: File): ConfigureNative {
        val source = root.resolve("native")
        source.mkdirs()
        val cmakeLists = source.resolve("CMakeLists.txt")
        cmakeLists.writeText(PROBE_PROJECT)
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("configureNative", ConfigureNative::class.java).get()
        val build = root.resolve("build/cmake")
        task.sourceDirectory.set(source)
        task.cmakeListsFile.set(cmakeLists)
        task.buildType.set("Debug")
        task.javaHome.set(System.getProperty("java.home"))
        task.libraryIncludePaths.set(emptyList())
        task.cmakeBuildDirectory.set(build)
        task.libraryOutputDirectory.set(root.resolve("build/lib"))
        task.cmakeCache.set(build.resolve("CMakeCache.txt"))
        return task
    }

    fun cacheOf(task: ConfigureNative): String {
        val cache = task.cmakeCache.get()
        return cache.asFile.readText()
    }

    context("ConfigureNative") {
        test("configures the build type, Java home and library output directory")
            .config(enabledIf = { isCmakeInstalled }) {
                val root = workspace.freshDirectory("jni")
                val task = configureTask(root)
                task.generator.set("  ")

                task.execute()

                val cache = cacheOf(task)
                cache shouldContain "CMAKE_BUILD_TYPE:UNINITIALIZED=Debug"
                cache shouldContain "CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE"
                cache shouldContain "JAVA_HOME"
            }

        test("hands the generated headers and the library includes to the build as one list")
            .config(enabledIf = { isCmakeInstalled }) {
                val root = workspace.freshDirectory("jni")
                val headers = root.resolve("headers")
                headers.mkdirs()
                val task = configureTask(root)
                task.generatedHeaderDirectory.set(headers)
                task.libraryIncludePaths.set(listOf("/opt/acme/include", " "))

                task.execute()

                cacheOf(task) shouldContain "KREATE_JNI_INCLUDE_DIRS:UNINITIALIZED=${headers.path};/opt/acme/include"
            }

        test("uses the configured generator").config(enabledIf = { isCmakeInstalled && isMakeInstalled }) {
            val root = workspace.freshDirectory("jni")
            val task = configureTask(root)
            task.generator.set("Unix Makefiles")

            task.execute()

            cacheOf(task) shouldContain "CMAKE_GENERATOR:INTERNAL=Unix Makefiles"
        }

        test("fails when CMake left no cache where the build expects it")
            .config(enabledIf = { isCmakeInstalled }) {
                val root = workspace.freshDirectory("jni")
                val task = configureTask(root)
                task.cmakeCache.set(root.resolve("elsewhere/CMakeCache.txt"))

                val failure = shouldThrow<GradleException> { task.execute() }

                failure.message shouldContain "CMake reported success but did not write"
            }
    }
})
