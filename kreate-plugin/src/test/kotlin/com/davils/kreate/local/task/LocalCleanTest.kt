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

package com.davils.kreate.local.task

import com.davils.kreate.capturedLogOf
import com.davils.kreate.freshDirectory
import com.davils.kreate.settings.local.LocalLibrary
import com.davils.kreate.settings.local.LocalModule
import com.davils.kreate.settings.local.groupDirectory
import com.davils.kreate.settings.local.moduleDirectory
import com.davils.kreate.settings.local.writeLocalLibrary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val VERSION = "1.0.0-SNAPSHOT"
private const val GROUP = "com.acme"

class LocalCleanTest : FunSpec({
    val workspace = tempdir()

    fun library(name: String): LocalLibrary = LocalLibrary(
        library = name,
        group = GROUP,
        repository = File("/work/$name"),
        version = VERSION,
        publishedAt = null,
        kreateVersion = "4.0.0",
        modules = listOf(LocalModule(GROUP, name))
    )

    fun installed(repository: File, module: String, version: String): File {
        val directory = moduleDirectory(repository, LocalModule(GROUP, module), version)
        directory.mkdirs()
        directory.resolve("$module-$version.jar").writeText("")
        return directory
    }

    fun cleanTask(state: File, repository: File, sweepAll: Boolean, isCi: Boolean): LocalClean {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("clean", LocalClean::class.java).get()
        task.stateDirectory.set(state)
        task.mavenLocal.set(repository)
        task.sweepAll.set(sweepAll)
        task.continuousIntegration.set(isCi)
        return task
    }

    context("LocalClean") {
        test("refuses to run in CI") {
            val task = cleanTask(workspace.freshDirectory("state"), workspace.freshDirectory("m2"), false, true)

            val failure = shouldThrow<IllegalStateException> { task.clean() }

            failure.message shouldContain "must not run in CI"
        }

        test("removes the one recorded publication, its record and the emptied state directory") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            writeLocalLibrary(state, library("core"))
            val published = installed(repository, "core", VERSION)
            val task = cleanTask(state, repository, false, false)

            val log = capturedLogOf { task.clean() }

            published.shouldNotExist()
            groupDirectory(repository, GROUP).shouldNotExist()
            state.shouldNotExist()
            log shouldContain "Removed 1 directory and 1 record(s)."
        }

        test("leaves other snapshots of the group alone unless asked to sweep") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            writeLocalLibrary(state, library("core"))
            installed(repository, "core", VERSION)
            val stale = installed(repository, "core", "0.9.0-SNAPSHOT")
            val task = cleanTask(state, repository, false, false)

            capturedLogOf { task.clean() }

            stale.shouldExist()
        }

        test("sweeps every snapshot of the recorded groups when asked to") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            writeLocalLibrary(state, library("core"))
            installed(repository, "core", VERSION)
            val stale = installed(repository, "core", "0.9.0-SNAPSHOT")
            val release = installed(repository, "core", "0.8.0")
            val task = cleanTask(state, repository, true, false)

            val log = capturedLogOf { task.clean() }

            stale.shouldNotExist()
            release.shouldExist()
            log shouldContain "Removed 2 directories and 1 record(s)."
        }

        test("keeps a state directory that still holds other files") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            writeLocalLibrary(state, library("core"))
            state.resolve("notes.txt").writeText("keep")
            installed(repository, "core", VERSION)
            val task = cleanTask(state, repository, false, false)

            capturedLogOf { task.clean() }

            state.resolve("notes.txt").shouldExist()
        }
    }
})
