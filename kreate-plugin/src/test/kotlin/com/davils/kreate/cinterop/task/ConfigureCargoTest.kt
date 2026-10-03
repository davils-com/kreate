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
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val MANIFEST = "[package]\nname = \"demo\"\n"

class ConfigureCargoTest : FunSpec({
    val workspace = tempdir()

    fun cargoTask(rustProject: File): ConfigureCargo {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("cargo", ConfigureCargo::class.java).get()
        task.workDir.set(rustProject)
        return task
    }

    context("ConfigureCargo") {
        test("turns the crate into a static library") {
            val rustProject = workspace.freshDirectory("rust")
            rustProject.resolve(CARGO_TOML_FILE_NAME).writeText(MANIFEST)
            val task = cargoTask(rustProject)

            task.execute()

            val manifest = task.outputFile.readText()
            manifest shouldStartWith MANIFEST
            manifest shouldEndWith "crate-type = [\"staticlib\"]"
        }

        test("leaves a manifest that already builds a static library untouched") {
            val rustProject = workspace.freshDirectory("rust")
            val task = cargoTask(rustProject)
            val configured = MANIFEST + task.extendedCargoContent
            rustProject.resolve(CARGO_TOML_FILE_NAME).writeText(configured)

            task.execute()

            task.outputFile.readText() shouldBe configured
        }

        test("does nothing before Cargo has created the project") {
            val rustProject = workspace.freshDirectory("rust")
            val task = cargoTask(rustProject)

            task.execute()

            task.outputFile.shouldNotExist()
        }
    }
})
