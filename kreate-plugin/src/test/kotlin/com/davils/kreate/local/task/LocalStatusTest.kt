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
import com.davils.kreate.settings.local.moduleDirectory
import com.davils.kreate.settings.local.writeLocalLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.time.Duration
import java.time.Instant

private const val VERSION = "1.0.0-SNAPSHOT"

class LocalStatusTest : FunSpec({
    val workspace = tempdir()

    fun library(name: String, publishedAt: String?, modules: List<LocalModule>): LocalLibrary = LocalLibrary(
        library = name,
        group = "com.acme",
        repository = File("/work/$name"),
        version = VERSION,
        publishedAt = publishedAt,
        kreateVersion = "4.0.0",
        modules = modules
    )

    fun modulesOf(name: String): List<LocalModule> = listOf(LocalModule("com.acme", name))

    fun ago(duration: Duration): String {
        val now = Instant.now()
        return now.minus(duration).toString()
    }

    fun install(repository: File, library: LocalLibrary) {
        library.modules.forEach { module ->
            val directory = moduleDirectory(repository, module, library.version)
            directory.mkdirs()
        }
    }

    fun statusOf(state: File, repository: File, inactiveReason: String?): String {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("status", LocalStatus::class.java).get()
        task.stateDirectory.set(state)
        task.mavenLocal.set(repository)
        task.inactiveReason.set(inactiveReason)
        return capturedLogOf { task.report() }
    }

    context("LocalStatus") {
        test("explains how to publish when nothing is published locally") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")

            val status = statusOf(state, repository, null)

            status shouldContain "Nothing is published locally."
            status shouldContain "./gradlew kreateLocalPublish"
        }

        test("lists a fully installed library and reports local mode as on") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            val core = library("core", ago(Duration.ofSeconds(5)), listOf(LocalModule("com.acme", "core")))
            writeLocalLibrary(state, core)
            install(repository, core)

            val status = statusOf(state, repository, null)

            status shouldContain "1 library is published locally:"
            status shouldContain "just now"
            status shouldContain "1 module(s), published by Kreate 4.0.0"
            status shouldContain "Local mode is ON."
        }

        test("reports the age of every library in the largest fitting unit") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            writeLocalLibrary(state, library("minutes", ago(Duration.ofMinutes(5)), modulesOf("minutes")))
            writeLocalLibrary(state, library("hours", ago(Duration.ofHours(3)), modulesOf("hours")))
            writeLocalLibrary(state, library("days", ago(Duration.ofDays(2)), modulesOf("days")))
            writeLocalLibrary(state, library("garbled", "yesterday", modulesOf("garbled")))

            val status = statusOf(state, repository, null)

            status shouldContain "4 libraries are published locally:"
            status shouldContain "5 min ago"
            status shouldContain "3 h ago"
            status shouldContain "2 d ago"
            status shouldContain "unknown"
        }

        test("warns about missing artifacts, shared coordinates and an inactive local mode") {
            val state = workspace.freshDirectory("state")
            val repository = workspace.freshDirectory("m2")
            val shared = LocalModule("com.acme", "shared")
            writeLocalLibrary(state, library("first", ago(Duration.ofSeconds(1)), listOf(shared)))
            writeLocalLibrary(state, library("second", ago(Duration.ofSeconds(1)), listOf(shared)))

            val status = statusOf(state, repository, "the build runs in CI")

            status shouldContain "recorded coordinate(s) are not installed: com.acme:shared:$VERSION"
            status shouldContain "claimed by more than one library"
            status shouldContain "Local mode is OFF for this build, because the build runs in CI."
        }
    }
})
