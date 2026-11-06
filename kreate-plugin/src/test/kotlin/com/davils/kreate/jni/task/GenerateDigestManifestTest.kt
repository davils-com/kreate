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
package com.davils.kreate.jni.task

import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.security.MessageDigest

private const val PLATFORM: String = "windows-x86_64"

private fun sha256Of(content: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(content.toByteArray())
    return hash.joinToString("") { byte -> "%02x".format(byte) }
}

private fun File.library(name: String, content: String) {
    mkdirs()
    val library = resolve(name)
    library.writeText(content)
}

class GenerateDigestManifestTest : FunSpec({
    val workspace = tempdir()

    fun digestEntries(vararg directories: File): List<String> {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("digests", GenerateDigestManifest::class.java).get()
        val manifest = workspace.freshDirectory("manifest").resolve("digests.properties")
        task.libraryDirectories.from(directories)
        task.platformId.set(PLATFORM)
        task.manifest.set(manifest)

        task.execute()

        return manifest.readLines().filterNot { line -> line.startsWith("#") }
    }

    context("GenerateDigestManifest") {
        test("gives every file its own key, so a second file cannot overwrite the first") {
            val directory = workspace.freshDirectory("libraries")
            directory.library("sample.dll", "library")
            directory.library("sample.lib", "import library")

            digestEntries(directory) shouldContainExactlyInAnyOrder listOf(
                "$PLATFORM/sample.dll=${sha256Of("library")}",
                "$PLATFORM/sample.lib=${sha256Of("import library")}"
            )
        }

        test("reports the file the JAR takes when two directories hold the same name") {
            val staged = workspace.freshDirectory("staged")
            staged.library("sample.dll", "staged")
            val hostBuilt = workspace.freshDirectory("hostBuilt")
            hostBuilt.library("sample.dll", "host built")

            digestEntries(staged, hostBuilt) shouldContainExactlyInAnyOrder listOf(
                "$PLATFORM/sample.dll=${sha256Of("staged")}"
            )
        }

        test("skips a directory that does not exist, as a staging directory may not") {
            val hostBuilt = workspace.freshDirectory("hostBuilt")
            hostBuilt.library("sample.dll", "host built")
            val missing = workspace.resolve("missing")

            digestEntries(missing, hostBuilt) shouldContainExactlyInAnyOrder listOf(
                "$PLATFORM/sample.dll=${sha256Of("host built")}"
            )
        }
    }
})
