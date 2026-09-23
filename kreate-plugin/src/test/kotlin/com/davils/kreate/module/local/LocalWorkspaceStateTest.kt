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

package com.davils.kreate.module.local

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the file that records what is published to the local Maven repository.
 *
 * The file is the contract between a producer in one repository and a consumer in another. It is
 * also written by two separate implementations — this one, and the copy in `build-logic` that
 * exists because Kreate cannot apply itself — so the format has to be pinned by tests rather than
 * by whichever side was edited last.
 */
@DisplayName("local workspace state")
class LocalWorkspaceStateTest {

    @TempDir
    lateinit var stateDirectory: File

    private fun library(
        name: String = "rise",
        version: String = "1.1.0-SNAPSHOT",
        repository: File = File("/workspace/rise"),
        modules: List<LocalModule> = listOf(
            LocalModule("com.davils", "rise-bom"),
            LocalModule("com.davils", "rise-core"),
            LocalModule("com.davils", "rise-core-jvm")
        )
    ) = LocalLibrary(
        library = name,
        group = "com.davils",
        repository = repository,
        version = version,
        publishedAt = "2026-09-23T14:02:11Z",
        kreateVersion = "3.2.0",
        modules = modules
    )

    @Nested
    @DisplayName("writing and reading back")
    inner class RoundTrip {

        @Test
        @DisplayName("preserves every recorded field")
        fun roundTrip() {
            val written = library()
            writeLocalLibrary(stateDirectory, written)

            val read = readLocalWorkspace(stateDirectory).libraries.single()

            read.library shouldBe written.library
            read.group shouldBe written.group
            read.version shouldBe written.version
            read.kreateVersion shouldBe written.kreateVersion
            read.publishedAt shouldBe written.publishedAt
            read.repository.absolutePath shouldBe written.repository.absolutePath
            read.modules shouldContainExactly written.modules
        }

        @Test
        @DisplayName("survives a repository path outside Latin-1")
        fun unicodePath() {
            // `Properties.store(OutputStream)` escapes everything outside Latin-1, which would
            // corrupt a path the moment someone checks out into a directory with an umlaut.
            val path = File("/workspace/prüfung-日本/rise")
            writeLocalLibrary(stateDirectory, library(repository = path))

            val read = readLocalWorkspace(stateDirectory).libraries.single()

            read.repository.absolutePath shouldBe path.absolutePath
        }

        @Test
        @DisplayName("keys one file per library, so two producers cannot collide")
        fun oneFilePerLibrary() {
            writeLocalLibrary(stateDirectory, library(name = "rise"))
            writeLocalLibrary(
                stateDirectory,
                library(name = "arc", modules = listOf(LocalModule("com.davils", "arc")))
            )

            readLocalWorkspace(stateDirectory).libraries.map { it.library } shouldBe
                listOf("arc", "rise")
        }

        @Test
        @DisplayName("replaces the previous record of the same library")
        fun replaces() {
            writeLocalLibrary(stateDirectory, library(version = "1.1.0-SNAPSHOT"))
            writeLocalLibrary(stateDirectory, library(version = "1.2.0-SNAPSHOT"))

            readLocalWorkspace(stateDirectory).libraries.single().version shouldBe "1.2.0-SNAPSHOT"
        }

        @Test
        @DisplayName("leaves no temporary file behind")
        fun noTemporaryLeftover() {
            writeLocalLibrary(stateDirectory, library())

            stateDirectory.listFiles().orEmpty()
                .map { it.name }
                .none { it.endsWith(".tmp") } shouldBe true
        }
    }

    @Nested
    @DisplayName("refusing to record")
    inner class Refusals {

        @Test
        @DisplayName("a release version, which could shadow a real one")
        fun releaseVersion() {
            shouldThrow<IllegalArgumentException> {
                writeLocalLibrary(stateDirectory, library(version = "1.1.0"))
            }
        }
    }

    @Nested
    @DisplayName("reading a directory that is not intact")
    inner class Damaged {

        @Test
        @DisplayName("treats a missing directory as nothing published")
        fun missingDirectory() {
            readLocalWorkspace(File(stateDirectory, "does-not-exist")).isEmpty shouldBe true
        }

        @Test
        @DisplayName("skips a truncated file rather than blocking every build on the machine")
        fun truncatedFile() {
            writeLocalLibrary(stateDirectory, library())
            File(stateDirectory, "com.davils.broken$STATE_EXTENSION")
                .writeText("library=broken\ngroup=com.davils\n")

            // The intact record still resolves; the incomplete one is simply absent.
            readLocalWorkspace(stateDirectory).libraries.map { it.library } shouldBe listOf("rise")
        }

        @Test
        @DisplayName("skips a record naming no modules, which would substitute nothing")
        fun noModules() {
            File(stateDirectory, "com.davils.empty$STATE_EXTENSION")
                .writeText("library=empty\ngroup=com.davils\nversion=1.0.0-SNAPSHOT\nmodules=\n")

            readLocalWorkspace(stateDirectory).isEmpty shouldBe true
        }

        @Test
        @DisplayName("ignores files that are not state files")
        fun foreignFiles() {
            writeLocalLibrary(stateDirectory, library())
            File(stateDirectory, "README.md").writeText("not a record")

            readLocalWorkspace(stateDirectory).libraries.size shouldBe 1
        }
    }

    @Nested
    @DisplayName("the derived substitution map")
    inner class Substitutions {

        @Test
        @DisplayName("maps every published module to its library's version")
        fun perModule() {
            val workspace = LocalWorkspace(listOf(library()))

            workspace.substitutions shouldBe mapOf(
                "com.davils:rise-bom" to "1.1.0-SNAPSHOT",
                "com.davils:rise-core" to "1.1.0-SNAPSHOT",
                "com.davils:rise-core-jvm" to "1.1.0-SNAPSHOT"
            )
        }

        @Test
        @DisplayName("reports a coordinate claimed by two libraries")
        fun duplicates() {
            val workspace = LocalWorkspace(
                listOf(
                    library(name = "rise", modules = listOf(LocalModule("com.davils", "shared"))),
                    library(name = "leaf", modules = listOf(LocalModule("com.davils", "shared")))
                )
            )

            workspace.duplicateCoordinates shouldContainExactly listOf("com.davils:shared")
        }
    }

    @Nested
    @DisplayName("coordinate parsing")
    inner class Coordinates {

        @Test
        @DisplayName("accepts a group and a name")
        fun accepts() {
            LocalModule.parse(" com.davils : rise-core ") shouldBe
                LocalModule("com.davils", "rise-core")
        }

        @Test
        @DisplayName("rejects anything that is not exactly one pair")
        fun rejects() {
            LocalModule.parse("com.davils") shouldBe null
            LocalModule.parse("com.davils:rise:1.0.0") shouldBe null
            LocalModule.parse(":rise") shouldBe null
            LocalModule.parse("com.davils:") shouldBe null
        }

        @Test
        @DisplayName("renders back to the form it was parsed from")
        fun renders() {
            LocalModule("com.davils", "rise-core").coordinate shouldBe "com.davils:rise-core"
        }
    }

    @Nested
    @DisplayName("the state file location")
    inner class Location {

        @Test
        @DisplayName("lives under the Gradle user home, where CI cannot carry it between jobs")
        fun underGradleUserHome() {
            val file = stateFileOf(stateDirectory, "com.davils", "rise")

            file.parentFile shouldBe stateDirectory
            file.name shouldBe "com.davils.rise.properties"
            file.absolutePath shouldNotBe null
        }
    }
}
