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
import com.davils.kreate.local.workspace.WorkspaceLibrary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val PUBLISH_TASK = "publishToMavenLocal"

class LocalPublishAllTest : FunSpec({
    val workspace = tempdir()

    fun repository(root: File, name: String, exitCode: Int): WorkspaceLibrary {
        val directory = root.resolve(name)
        directory.mkdirs()
        val journal = root.resolve("journal.txt")
        val unixWrapper = directory.resolve("gradlew")
        unixWrapper.writeText("#!/bin/sh\necho $name >> '${journal.path}'\nexit $exitCode\n")
        unixWrapper.setExecutable(true)
        val batchLines = listOf("@echo $name>> \"${journal.path}\"", "@exit /b $exitCode", "")
        directory.resolve("gradlew.bat").writeText(batchLines.joinToString("\r\n"))
        return WorkspaceLibrary(name, directory.path, emptyList(), listOf(PUBLISH_TASK))
    }

    fun publishAllTask(libraries: List<WorkspaceLibrary>): LocalPublishAll {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("publishAll", LocalPublishAll::class.java).get()
        task.libraries.set(libraries)
        return task
    }

    fun journalOf(root: File): List<String> {
        val journal = root.resolve("journal.txt")
        val lines = journal.readLines()
        return lines.map { line -> line.trim() }
    }

    context("LocalPublishAll") {
        test("does nothing when the workspace declares no library") {
            val task = publishAllTask(emptyList())

            val log = capturedLogOf { task.publishAll() }

            log shouldContain "Nothing selected. The workspace declares no libraries."
        }

        test("publishes exactly the libraries named with --only") {
            val root = workspace.freshDirectory("workspace")
            val core = repository(root, "core", 0)
            val app = repository(root, "app", 0)
            val task = publishAllTask(listOf(core, app))
            task.only.set(listOf(" app , "))

            val log = capturedLogOf { task.publishAll() }

            journalOf(root) shouldBe listOf("app")
            log shouldContain "Published 1 repositories: app."
            log shouldNotContain "> core"
        }

        test("refuses to combine --from with --only") {
            val task = publishAllTask(emptyList())
            task.from.set(listOf("core"))
            task.only.set(listOf("app"))

            val failure = shouldThrow<GradleException> { task.publishAll() }

            failure.message shouldContain "--from and --only mean different things and cannot be combined."
        }

        test("names the declared libraries when --only asks for one the workspace lacks") {
            val root = workspace.freshDirectory("workspace")
            val task = publishAllTask(listOf(repository(root, "core", 0), repository(root, "app", 0)))
            task.only.set(listOf("missing"))

            val failure = shouldThrow<GradleException> { task.publishAll() }

            failure.message shouldContain "The workspace does not declare missing. It declares: app, core."
        }

        test("stops at the first failing library and names the ones not run") {
            val root = workspace.freshDirectory("workspace")
            val core = repository(root, "core", 3)
            val app = repository(root, "app", 0)
            val task = publishAllTask(listOf(core, app.copy(dependencies = listOf("core"))))

            val failure = shouldThrow<GradleException> { capturedLogOf { task.publishAll() } }

            failure.message shouldContain "Publishing 'core' failed (exit 3)."
            failure.message shouldContain "Not run:    app"
            journalOf(root) shouldBe listOf("core")
        }

        test("fails on a library without a Gradle wrapper") {
            val root = workspace.freshDirectory("workspace")
            val bare = WorkspaceLibrary("bare", root.path, emptyList(), listOf(PUBLISH_TASK))
            val task = publishAllTask(listOf(bare))

            val failure = shouldThrow<GradleException> { capturedLogOf { task.publishAll() } }

            failure.message shouldContain "No Gradle wrapper in"
        }
    }
})
