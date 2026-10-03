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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File

class MavenLocalTest : FunSpec({

    val workspace = tempdir()

    fun artifact(repository: File, path: String) {
        File(repository, path).apply {
            mkdirs()
            File(this, "${name.substringBeforeLast('/')}.pom").writeText("<project/>")
        }
    }

    fun library(vararg modules: String) = LocalLibrary(
        library = "library",
        group = "com.example",
        repository = File("/workspace/library"),
        version = "1.1.0-SNAPSHOT",
        publishedAt = null,
        kreateVersion = "3.2.0",
        modules = modules.map { LocalModule("com.example", it) }
    )

    context("local Maven repository") {

        context("resolution order") {

            test("prefers the maven.repo.local system property") {
                val home = workspace.freshDirectory()
                val configured = File(home, "explicit")

                resolveMavenLocal(
                    mapOf(MAVEN_REPO_LOCAL_PROPERTY to configured.absolutePath),
                    home
                ) shouldBe configured.absoluteFile
            }

            test("falls back to localRepository in settings.xml") {
                val home = workspace.freshDirectory()
                val configured = File(home, "from-settings")
                File(home, ".m2").mkdirs()
                File(home, ".m2/settings.xml").writeText(
                    """
                        <settings>
                            <localRepository>${configured.absolutePath}</localRepository>
                        </settings>
                    """.trimIndent()
                )

                resolveMavenLocal(emptyMap(), home) shouldBe configured.absoluteFile
            }

            test("expands a leading tilde in settings.xml") {
                val home = workspace.freshDirectory()
                File(home, ".m2").mkdirs()
                File(home, ".m2/settings.xml").writeText(
                    "<settings><localRepository>~/custom-repo</localRepository></settings>"
                )

                resolveMavenLocal(emptyMap(), home) shouldBe File(home, "custom-repo").absoluteFile
            }

            test("falls back to ~/.m2/repository when nothing is configured") {
                val home = workspace.freshDirectory()

                resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
            }

            test("falls back when settings.xml configures no local repository") {
                val home = workspace.freshDirectory()
                File(home, ".m2").mkdirs()
                File(home, ".m2/settings.xml").writeText("<settings><offline>true</offline></settings>")

                resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
            }

            test("falls back when the local repository in settings.xml is blank") {
                val home = workspace.freshDirectory()
                File(home, ".m2").mkdirs()
                File(home, ".m2/settings.xml").writeText("<settings><localRepository> </localRepository></settings>")

                resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
            }

            test("ignores a blank maven.repo.local system property") {
                val home = workspace.freshDirectory()

                resolveMavenLocal(mapOf(MAVEN_REPO_LOCAL_PROPERTY to " "), home) shouldBe
                    File(home, ".m2/repository")
            }

            test("falls back rather than failing on a malformed settings.xml") {
                val home = workspace.freshDirectory()
                File(home, ".m2").mkdirs()
                File(home, ".m2/settings.xml").writeText("<settings><localRepository>")

                resolveMavenLocal(emptyMap(), home) shouldBe File(home, ".m2/repository")
            }
        }

        context("reading the build's system properties") {

            test("honours maven.repo.local") {
                val home = workspace.freshDirectory()
                val configured = File(home, "explicit")
                val providers = fakeProviders(
                    systemProperties = mapOf(MAVEN_REPO_LOCAL_PROPERTY to configured.path, "user.home" to home.path)
                )

                mavenLocalOf(providers) shouldBe configured.absoluteFile
            }

            test("falls back to the repository below user.home") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(systemProperties = mapOf("user.home" to home.path))

                mavenLocalOf(providers) shouldBe File(home, ".m2/repository")
            }
        }

        context("locating a module") {

            test("maps a group onto its directory path") {
                val home = workspace.freshDirectory()

                groupDirectory(home, "com.example") shouldBe File(home, "com/example")
            }

            test("maps a module version onto its directory") {
                val home = workspace.freshDirectory()
                val module = LocalModule("com.example", "library-core")

                moduleDirectory(home, module, "1.1.0-SNAPSHOT") shouldBe
                    File(home, "com/example/library-core/1.1.0-SNAPSHOT")
            }
        }

        context("verifying a recorded library") {

            test("reports nothing missing when every coordinate is installed") {
                val home = workspace.freshDirectory()
                File(home, "com/example/library-core/1.1.0-SNAPSHOT").mkdirs()

                missingArtifacts(home, library("library-core")) shouldBe emptyList()
            }

            test("names the coordinates that were removed after the record was written") {
                val home = workspace.freshDirectory()
                File(home, "com/example/library-core/1.1.0-SNAPSHOT").mkdirs()

                missingArtifacts(home, library("library-core", "library-retry")) shouldContainExactly
                    listOf("com.example:library-retry:1.1.0-SNAPSHOT")
            }
        }

        context("finding snapshots to purge") {

            test("finds a version directory nested below a same-named group") {
                val home = workspace.freshDirectory()
                artifact(home, "com/example/tool/3.2.0-SNAPSHOT")
                artifact(home, "com/example/tool/com.example.tool.gradle.plugin/3.2.0-SNAPSHOT")

                val foundWithPortableSeparators = snapshotDirectories(home, "com.example")
                    .map { it.relativeTo(home).invariantSeparatorsPath }

                foundWithPortableSeparators shouldContainExactly listOf(
                    "com/example/tool/3.2.0-SNAPSHOT",
                    "com/example/tool/com.example.tool.gradle.plugin/3.2.0-SNAPSHOT"
                )
            }

            test("leaves releases alone, because something else put them there") {
                val home = workspace.freshDirectory()
                artifact(home, "com/example/extra/3.0.0")
                artifact(home, "com/example/extra/3.0.0-SNAPSHOT")

                snapshotDirectories(home, "com.example").map { it.name } shouldContainExactly
                    listOf("3.0.0-SNAPSHOT")
            }

            test("ignores an empty directory that merely looks like a version") {
                val home = workspace.freshDirectory()
                File(home, "com/example/weird-SNAPSHOT").mkdirs()

                snapshotDirectories(home, "com.example") shouldBe emptyList()
            }

            test("returns nothing for a group that was never published") {
                val home = workspace.freshDirectory()

                snapshotDirectories(home, "com.example") shouldBe emptyList()
            }
        }
    }
})
