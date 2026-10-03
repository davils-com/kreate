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
import org.gradle.testfixtures.ProjectBuilder

class CompileRustTest : FunSpec({
    val workspace = tempdir()

    context("CompileRust") {
        test("builds into Cargo's target directory of the Rust project") {
            val builder = ProjectBuilder.builder()
            builder.withProjectDir(workspace.freshDirectory("project"))
            val project = builder.build()
            val rustProject = workspace.freshDirectory("rust")
            val task = project.tasks.register("compile", CompileRust::class.java).get()
            task.workDir.set(rustProject)

            task.outputDir shouldBe rustProject.resolve("target")
        }
    }
})
