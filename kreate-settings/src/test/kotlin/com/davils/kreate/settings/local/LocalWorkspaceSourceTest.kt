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

package com.davils.kreate.settings.local

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class LocalWorkspaceSourceTest : FunSpec({

    val workspace = tempdir()

    fun providers(): ProviderFactory {
        val builder = ProjectBuilder.builder()
        val project = builder.withProjectDir(workspace.freshDirectory("project")).build()
        return project.providers
    }

    fun library(name: String) = LocalLibrary(
        library = name,
        group = "com.example",
        repository = File("/workspace/$name"),
        version = "1.0.0-SNAPSHOT",
        publishedAt = "2026-09-23T14:02:11Z",
        kreateVersion = "3.2.0",
        modules = listOf(LocalModule("com.example", name))
    )

    context("localWorkspaceProvider") {

        test("reads the libraries recorded in the state directory") {
            val stateDirectory = workspace.freshDirectory()
            writeLocalLibrary(stateDirectory, library("net"))
            writeLocalLibrary(stateDirectory, library("core"))

            val read = localWorkspaceProvider(providers(), stateDirectory).get()

            read.libraries.map { it.library } shouldBe listOf("core", "net")
        }

        test("yields an empty workspace for a state directory that does not exist") {
            val missing = File(workspace.freshDirectory(), "missing")

            localWorkspaceProvider(providers(), missing).get() shouldBe LocalWorkspace.EMPTY
        }
    }
})
