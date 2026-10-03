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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class GenerateRustBuildScriptTest : FunSpec({
    val workspace = tempdir()

    fun scriptTask(rustProject: File): GenerateRustBuildScript {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("script", GenerateRustBuildScript::class.java).get()
        task.workDir.set(rustProject)
        task.projectName.set("demo")
        return task
    }

    context("GenerateRustBuildScript") {
        test("writes a cbindgen build script that emits the project's header") {
            val rustProject = workspace.freshDirectory("rust")
            val task = scriptTask(rustProject)

            task.execute()

            val script = task.outputFile.readText()
            script shouldContain "extern crate cbindgen;"
            script shouldContain "write_to_file(\"include/demo.h\")"
        }

        test("keeps a build script the project already wrote") {
            val rustProject = workspace.freshDirectory("rust")
            rustProject.resolve(BUILD_RUST_FILE_NAME).writeText("fn main() {}")
            val task = scriptTask(rustProject)

            task.execute()

            task.outputFile.readText() shouldBe "fn main() {}"
        }

        test("replaces an empty build script") {
            val rustProject = workspace.freshDirectory("rust")
            rustProject.resolve(BUILD_RUST_FILE_NAME).writeText("")
            val task = scriptTask(rustProject)

            task.execute()

            task.outputFile.readText() shouldContain "cbindgen::Builder::new()"
        }

        test("writes the script next to Cargo.toml") {
            val rustProject = workspace.freshDirectory("rust")

            scriptTask(rustProject).outputFile shouldBe rustProject.resolve("build.rs")
        }
    }
})
