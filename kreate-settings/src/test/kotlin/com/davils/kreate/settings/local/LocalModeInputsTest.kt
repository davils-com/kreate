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
import java.io.File

class LocalModeInputsTest : FunSpec({

    val workspace = tempdir()

    val published = LocalWorkspace(
        listOf(
            LocalLibrary(
                library = "core",
                group = "com.example",
                repository = File("/workspace/core"),
                version = "1.0.0-SNAPSHOT",
                publishedAt = null,
                kreateVersion = "3.2.0",
                modules = listOf(LocalModule(group = "com.example", name = "core"))
            )
        )
    )

    context("gatherLocalModeInputs") {

        context("a machine where nothing is configured") {

            test("reports no request, no filter and no CI") {
                val home = workspace.freshDirectory()

                val inputs = gatherLocalModeInputs(fakeProviders(), home, DEFAULT_CI_VARIABLES)

                inputs.requested shouldBe null
                inputs.only shouldBe emptySet()
                inputs.continuousIntegration shouldBe false
                inputs.ciVariable shouldBe null
            }

            test("reads the state directory below the Gradle user home") {
                val home = workspace.freshDirectory()

                val inputs = gatherLocalModeInputs(fakeProviders(), home, DEFAULT_CI_VARIABLES)

                inputs.stateDirectory shouldBe File(home, STATE_DIRECTORY).absolutePath
            }

            test("carries the workspace the value source reads") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(workspace = published)

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).workspace shouldBe published
            }
        }

        context("the local switch") {

            test("is read from the Gradle property") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(gradleProperties = mapOf(LOCAL_PROPERTY to "true"))

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).requested shouldBe true
            }

            test("falls back to the environment variable") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(environment = mapOf(LOCAL_VARIABLE to "false"))

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).requested shouldBe false
            }

            test("prefers the Gradle property over the environment variable") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(
                    gradleProperties = mapOf(LOCAL_PROPERTY to "false"),
                    environment = mapOf(LOCAL_VARIABLE to "true")
                )

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).requested shouldBe false
            }
        }

        context("continuous integration") {

            test("names the first configured variable that is set") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(environment = mapOf("GITLAB_CI" to "true", "CI" to "true"))

                val inputs = gatherLocalModeInputs(providers, home, listOf("GITLAB_CI", "CI"))

                inputs.continuousIntegration shouldBe true
                inputs.ciVariable shouldBe "GITLAB_CI"
            }

            test("ignores a variable that is set but blank") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(environment = mapOf("CI" to "  "))

                gatherLocalModeInputs(providers, home, listOf("CI")).continuousIntegration shouldBe false
            }

            test("only looks at the configured variables") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(environment = mapOf("CI" to "true"))

                gatherLocalModeInputs(providers, home, listOf("BUILDKITE")).ciVariable shouldBe null
            }
        }

        context("the remaining properties") {

            test("relocate the state directory to an absolute path") {
                val home = workspace.freshDirectory()
                val relocated = File(workspace.freshDirectory(), "state")
                val providers = fakeProviders(
                    gradleProperties = mapOf(STATE_DIRECTORY_PROPERTY to " ${relocated.path} ")
                )

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).stateDirectory shouldBe
                    relocated.absolutePath
            }

            test("ignore a blank state directory override") {
                val home = workspace.freshDirectory()

                stateDirectoryOf(fakeProviders(mapOf(STATE_DIRECTORY_PROPERTY to " ")), home) shouldBe
                    File(home, STATE_DIRECTORY)
            }

            test("narrow the libraries with a comma separated list") {
                val home = workspace.freshDirectory()
                val providers = fakeProviders(gradleProperties = mapOf(LOCAL_ONLY_PROPERTY to " core, ,net "))

                gatherLocalModeInputs(providers, home, DEFAULT_CI_VARIABLES).only shouldBe setOf("core", "net")
            }
        }
    }
})
