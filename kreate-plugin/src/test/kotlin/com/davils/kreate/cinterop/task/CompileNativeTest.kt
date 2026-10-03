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

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class CompileNativeTest : FunSpec({
    val workspace = tempdir()

    fun compileTask(nativeProject: File): CompileNative {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("compile", CompileNative::class.java).get()
        task.workDir.set(nativeProject)
        return task
    }

    context("CompileNative") {
        test("builds into the CMake build directory of the native project") {
            val nativeProject = workspace.freshDirectory("native")

            compileTask(nativeProject).outputDir shouldBe nativeProject.resolve("build")
        }

        test("fails when the build directory cannot be created") {
            val nativeProject = workspace.freshDirectory("native").resolve("demo")
            nativeProject.writeText("not a directory")
            val task = compileTask(nativeProject)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "Failed to create CMake build directory"
        }

        test("fails when the native project has nothing CMake can configure") {
            val nativeProject = workspace.freshDirectory("native")
            val task = compileTask(nativeProject)
            task.buildType.set("Debug")

            shouldThrow<GradleException> { task.execute() }
        }
    }
})
