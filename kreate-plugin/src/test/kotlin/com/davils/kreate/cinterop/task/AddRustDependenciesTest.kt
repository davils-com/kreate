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

private val DECLARED_MANIFEST: String = listOf(
    "[dependencies]",
    "libc = \"0.2\"",
    "serde = \"1\"",
    "",
    "[build-dependencies]",
    "cbindgen = \"0.27\""
).joinToString("\n")

class AddRustDependenciesTest : FunSpec({
    val workspace = tempdir()

    fun dependenciesTask(rustProject: File): AddRustDependencies {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("dependencies", AddRustDependencies::class.java).get()
        task.workDir.set(rustProject)
        return task
    }

    context("AddRustDependencies") {
        test("fails before Cargo has created the project") {
            val rustProject = workspace.freshDirectory("rust")
            val task = dependenciesTask(rustProject)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "Cargo.toml not found"
        }

        test("adds nothing when the default dependencies are already declared") {
            val rustProject = workspace.freshDirectory("rust")
            val manifest = rustProject.resolve(CARGO_TOML_FILE_NAME)
            manifest.writeText(DECLARED_MANIFEST)
            val task = dependenciesTask(rustProject)

            task.execute()

            manifest.readText() shouldBe DECLARED_MANIFEST
        }

        test("adds nothing when the configured dependencies are already declared") {
            val rustProject = workspace.freshDirectory("rust")
            val manifest = rustProject.resolve(CARGO_TOML_FILE_NAME)
            manifest.writeText(DECLARED_MANIFEST)
            val task = dependenciesTask(rustProject)
            task.rustDependencies.set(mapOf("serde" to "1"))
            task.rustBuildDependencies.set(mapOf("cbindgen" to "0.27"))

            task.execute()

            manifest.readText() shouldBe DECLARED_MANIFEST
        }
    }
})
