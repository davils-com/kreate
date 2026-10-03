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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

class LocalWorkspaceStateTest : FunSpec({

    val workspace = tempdir()

    fun library(
        name: String = "library",
        version: String = "1.1.0-SNAPSHOT",
        repository: File = File("/workspace/library"),
        modules: List<LocalModule> = listOf(
            LocalModule("com.example", "library-bom"),
            LocalModule("com.example", "library-core"),
            LocalModule("com.example", "library-core-jvm")
        )
    ) = LocalLibrary(
        library = name,
        group = "com.example",
        repository = repository,
        version = version,
        publishedAt = "2026-09-23T14:02:11Z",
        kreateVersion = "3.2.0",
        modules = modules
    )

    context("local workspace state") {

        context("writing and reading back") {

            test("preserves every recorded field") {
                val stateDirectory = workspace.freshDirectory()
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

            test("survives a repository path outside Latin-1") {
                val stateDirectory = workspace.freshDirectory()
                val pathOutsideLatin1 = File("/workspace/prüfung-日本/library")
                writeLocalLibrary(stateDirectory, library(repository = pathOutsideLatin1))

                val read = readLocalWorkspace(stateDirectory).libraries.single()

                read.repository.absolutePath shouldBe pathOutsideLatin1.absolutePath
            }

            test("keys one file per library, so two producers cannot collide") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library(name = "library"))
                writeLocalLibrary(
                    stateDirectory,
                    library(name = "extra", modules = listOf(LocalModule("com.example", "extra")))
                )

                readLocalWorkspace(stateDirectory).libraries.map { it.library } shouldBe
                    listOf("extra", "library")
            }

            test("replaces the previous record of the same library") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library(version = "1.1.0-SNAPSHOT"))
                writeLocalLibrary(stateDirectory, library(version = "1.2.0-SNAPSHOT"))

                readLocalWorkspace(stateDirectory).libraries.single().version shouldBe "1.2.0-SNAPSHOT"
            }

            test("records the time of writing when no publication time is given") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library().copy(publishedAt = null))

                val read = readLocalWorkspace(stateDirectory).libraries.single()

                read.publishedAtInstant() shouldNotBe null
            }

            test("leaves no temporary file behind") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library())

                val files = stateDirectory.listFiles().orEmpty()
                val names = files.map { it.name }

                names.none { it.endsWith(".tmp") } shouldBe true
            }
        }

        context("refusing to record") {

            test("a release version, which could shadow a real one") {
                val stateDirectory = workspace.freshDirectory()

                shouldThrow<IllegalArgumentException> {
                    writeLocalLibrary(stateDirectory, library(version = "1.1.0"))
                }
            }
        }

        context("reading a directory that is not intact") {

            test("treats a missing directory as nothing published") {
                val stateDirectory = workspace.freshDirectory()

                readLocalWorkspace(File(stateDirectory, "does-not-exist")).isEmpty shouldBe true
            }

            test("skips a truncated file rather than blocking every build on the machine") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library())
                File(stateDirectory, "com.example.broken$STATE_EXTENSION")
                    .writeText("library=broken\ngroup=com.example\n")

                val intactLibraries = readLocalWorkspace(stateDirectory).libraries.map { it.library }

                intactLibraries shouldBe listOf("library")
            }

            test("skips a record naming no modules, which would substitute nothing") {
                val stateDirectory = workspace.freshDirectory()
                File(stateDirectory, "com.example.empty$STATE_EXTENSION")
                    .writeText("library=empty\ngroup=com.example\nversion=1.0.0-SNAPSHOT\nmodules=\n")

                readLocalWorkspace(stateDirectory).isEmpty shouldBe true
            }

            test("skips a file with a malformed escape sequence") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library())
                File(stateDirectory, "com.example.escaped$STATE_EXTENSION")
                    .writeText("library=escaped\nmodules=\\u00zz\n")

                readLocalWorkspace(stateDirectory).libraries.map { it.library } shouldBe listOf("library")
            }

            test("skips a record that lacks its library, group or version") {
                val stateDirectory = workspace.freshDirectory()
                val complete = mapOf(
                    "library" to "partial",
                    "group" to "com.example",
                    "version" to "1.0.0-SNAPSHOT",
                    "modules" to "com.example:partial"
                )
                listOf("library", "group", "version").forEach { missing ->
                    val record = complete.filterKeys { key -> key != missing }
                    File(stateDirectory, "com.example.without-$missing$STATE_EXTENSION")
                        .writeText(record.entries.joinToString("\n") { (key, value) -> "$key=$value" })
                }

                readLocalWorkspace(stateDirectory).isEmpty shouldBe true
            }

            test("ignores a directory that is named like a state file") {
                val stateDirectory = workspace.freshDirectory()
                File(stateDirectory, "com.example.folder$STATE_EXTENSION").mkdirs()

                readLocalWorkspace(stateDirectory).isEmpty shouldBe true
            }

            test("reads a minimal record without repository and Kreate version") {
                val stateDirectory = workspace.freshDirectory()
                File(stateDirectory, "com.example.minimal$STATE_EXTENSION").writeText(
                    "library=minimal\ngroup=com.example\nversion=1.0.0-SNAPSHOT\nmodules=com.example:minimal\n"
                )

                val read = readLocalWorkspace(stateDirectory).libraries.single()

                read.repository shouldBe File("")
                read.kreateVersion shouldBe ""
                read.publishedAt shouldBe null
            }

            test("ignores files that are not state files") {
                val stateDirectory = workspace.freshDirectory()
                writeLocalLibrary(stateDirectory, library())
                File(stateDirectory, "README.md").writeText("not a record")

                readLocalWorkspace(stateDirectory).libraries.size shouldBe 1
            }
        }

        context("the derived substitution map") {

            test("maps every published module to its library's version") {
                val localWorkspace = LocalWorkspace(listOf(library()))

                localWorkspace.substitutions shouldBe mapOf(
                    "com.example:library-bom" to "1.1.0-SNAPSHOT",
                    "com.example:library-core" to "1.1.0-SNAPSHOT",
                    "com.example:library-core-jvm" to "1.1.0-SNAPSHOT"
                )
            }

            test("reports a coordinate claimed by two libraries") {
                val localWorkspace = LocalWorkspace(
                    listOf(
                        library(name = "library", modules = listOf(LocalModule("com.example", "shared"))),
                        library(name = "other", modules = listOf(LocalModule("com.example", "shared")))
                    )
                )

                localWorkspace.duplicateCoordinates shouldContainExactly listOf("com.example:shared")
            }
        }

        context("the state file location") {

            test("lives under the Gradle user home, where CI cannot carry it between jobs") {
                val stateDirectory = workspace.freshDirectory()
                val file = stateFileOf(stateDirectory, "com.example", "library")

                file.parentFile shouldBe stateDirectory
                file.name shouldBe "com.example.library.properties"
                file.absolutePath shouldNotBe null
            }
        }
    }
})
