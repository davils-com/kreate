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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val PROJECT_NAME = "demo"
private const val LINUX_TARGET = "x86_64-unknown-linux-gnu"
private const val MACOS_TARGET = "aarch64-apple-darwin"

class GenerateDefinitionFilesTest : FunSpec({
    val workspace = tempdir()

    fun definitionTask(rootDirectory: File, language: NativeLanguage): GenerateDefinitionFiles {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("definitions", GenerateDefinitionFiles::class.java).get()
        task.workDir.set(rootDirectory.resolve(PROJECT_NAME))
        task.rootDir.set(rootDirectory)
        task.projectName.set(PROJECT_NAME)
        task.defFileName.set("cinterop.def")
        task.defDirName.set("defs")
        task.language.set(language)
        return task
    }

    fun definitionOf(task: GenerateDefinitionFiles): String {
        val directory = task.outputDir
        return directory.resolve("cinterop.def").readText()
    }

    context("GenerateDefinitionFiles") {
        test("points a Rust definition at the release output of every declared target") {
            val root = workspace.freshDirectory("cinterop")
            val task = definitionTask(root, NativeLanguage.RUST)
            task.rustTargets.set(listOf(LINUX_TARGET, MACOS_TARGET))

            task.execute()

            val rootName = root.name
            val definition = definitionOf(task)
            definition shouldContain "headers = $PROJECT_NAME.h"
            definition shouldContain "staticLibraries = lib$PROJECT_NAME.a"
            definition shouldContain "compilerOpts = -I$rootName/$PROJECT_NAME/include"
            definition shouldContain "$rootName/$PROJECT_NAME/target/$LINUX_TARGET/release " +
                "$rootName/$PROJECT_NAME/target/$MACOS_TARGET/release"
        }

        test("points a C definition at the CMake build directory") {
            val root = workspace.freshDirectory("cinterop")
            val task = definitionTask(root, NativeLanguage.C)

            task.execute()

            definitionOf(task) shouldContain "libraryPaths = ${root.name}/$PROJECT_NAME/build"
        }

        test("points a C++ definition at the CMake build directory as well") {
            val root = workspace.freshDirectory("cinterop")
            val task = definitionTask(root, NativeLanguage.CPP)

            task.execute()

            definitionOf(task) shouldContain "libraryPaths = ${root.name}/$PROJECT_NAME/build"
        }

        test("rewrites a definition file that already exists") {
            val root = workspace.freshDirectory("cinterop")
            val task = definitionTask(root, NativeLanguage.C)
            val definitions = task.outputDir
            definitions.mkdirs()
            definitions.resolve("cinterop.def").writeText("stale")

            task.execute()

            definitionOf(task) shouldContain "headers = $PROJECT_NAME.h"
        }

        test("writes the definition below the configured directory") {
            val root = workspace.freshDirectory("cinterop")
            val task = definitionTask(root, NativeLanguage.C)

            task.outputDir shouldBe root.resolve(PROJECT_NAME).resolve("defs")
        }

        test("fails when the definition directory cannot be created") {
            val root = workspace.freshDirectory("cinterop")
            root.resolve(PROJECT_NAME).writeText("not a directory")
            val task = definitionTask(root, NativeLanguage.C)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "Failed to create cinterop directory"
        }
    }
})
