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
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class InitializeRustProjectTest : FunSpec({
    val workspace = tempdir()

    fun initializeTask(workDirectory: File): InitializeRustProject {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("initialize", InitializeRustProject::class.java).get()
        task.workDir.set(workDirectory)
        task.projectName.set("demo")
        return task
    }

    context("InitializeRustProject") {
        test("creates the Rust project below the working directory") {
            val root = workspace.freshDirectory("cinterop")

            initializeTask(root).outputDir shouldBe root.resolve("demo")
        }

        test("leaves an existing Rust project alone without calling Cargo") {
            val root = workspace.freshDirectory("cinterop")
            val rustProject = root.resolve("demo")
            rustProject.mkdirs()
            rustProject.resolve("lib.rs").writeText("")
            val task = initializeTask(root)

            task.execute()

            val entries = rustProject.list().orEmpty()
            entries.toList() shouldContainExactly listOf("lib.rs")
        }
    }
})
