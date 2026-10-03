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
import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class InitializeNativeProjectTest : FunSpec({
    val workspace = tempdir()

    fun initializeTask(workDirectory: File, language: NativeLanguage): InitializeNativeProject {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("initialize", InitializeNativeProject::class.java).get()
        task.workDir.set(workDirectory)
        task.projectName.set("demo")
        task.language.set(language)
        return task
    }

    context("InitializeNativeProject") {
        test("scaffolds a C project built as a static library") {
            val root = workspace.freshDirectory("cinterop")
            val task = initializeTask(root, NativeLanguage.C)

            task.execute()

            val project = task.outputDir
            val cmake = project.resolve("CMakeLists.txt").readText()
            cmake shouldContain "project(demo C)"
            cmake shouldContain "set(CMAKE_C_STANDARD 11)"
            cmake shouldContain "file(GLOB DEMO_SOURCES \"src/*.c\")"
            cmake shouldContain "add_library(demo STATIC \${DEMO_SOURCES})"
            project.resolve("src/demo.c").shouldExist()
            project.resolve("src/demo.cpp").shouldNotExist()
        }

        test("scaffolds a C++ project with a C header boundary") {
            val root = workspace.freshDirectory("cinterop")
            val task = initializeTask(root, NativeLanguage.CPP)

            task.execute()

            val project = task.outputDir
            val cmake = project.resolve("CMakeLists.txt").readText()
            cmake shouldContain "project(demo CXX)"
            cmake shouldContain "set(CMAKE_CXX_STANDARD 17)"
            val header = project.resolve("include/demo.h").readText()
            header shouldContain "#ifndef DEMO_H"
            header shouldContain "extern \"C\" {"
            header shouldContain "int demo_hello(void);"
            project.resolve("src/demo.cpp").readText() shouldContain "#include \"demo.h\""
        }

        test("keeps every file the project already has") {
            val root = workspace.freshDirectory("cinterop")
            val include = root.resolve("demo/include")
            include.mkdirs()
            include.resolve("demo.h").writeText("custom")
            val task = initializeTask(root, NativeLanguage.C)

            task.execute()

            include.resolve("demo.h").readText() shouldBe "custom"
        }

        test("refuses to scaffold a Rust project, which Cargo creates") {
            val root = workspace.freshDirectory("cinterop")
            val task = initializeTask(root, NativeLanguage.RUST)

            val failure = shouldThrow<IllegalStateException> { task.execute() }

            failure.message shouldContain "InitializeRustProject"
        }

        test("fails when a source directory cannot be created") {
            val root = workspace.freshDirectory("cinterop")
            root.resolve("demo").writeText("not a directory")
            val task = initializeTask(root, NativeLanguage.C)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "Failed to create src directory"
        }
    }
})
