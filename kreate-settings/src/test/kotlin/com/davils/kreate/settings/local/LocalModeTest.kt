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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.GradleException
import java.io.File

class LocalModeTest : FunSpec({

    fun workspace(vararg libraries: String): LocalWorkspace = LocalWorkspace(
        libraries.map { name ->
            LocalLibrary(
                library = name,
                group = "com.example",
                repository = File("/workspace/$name"),
                version = "1.0.0-SNAPSHOT",
                publishedAt = null,
                kreateVersion = "3.2.0",
                modules = listOf(LocalModule("com.example", name))
            )
        }
    )

    fun inputs(
        workspace: LocalWorkspace = LocalWorkspace.EMPTY,
        requested: Boolean? = null,
        only: Set<String> = emptySet(),
        ciVariable: String? = null
    ) = LocalModeInputs(
        workspace = workspace,
        stateDirectory = "/home/dev/.gradle/kreate/local",
        requested = requested,
        only = only,
        continuousIntegration = ciVariable != null,
        ciVariable = ciVariable
    )

    context("resolveLocalMode") {

        context("the explicit off switch") {

            test("wins over everything, including a populated state directory") {
                val mode = resolveLocalMode(inputs(workspace("core"), requested = false))

                mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "switched off"
            }

            test("wins over CI detection, so it never turns a refusal into a failure") {
                val mode = resolveLocalMode(
                    inputs(workspace("core"), requested = false, ciVariable = "GITLAB_CI")
                )

                mode.isActive shouldBe false
            }
        }

        context("continuous integration") {

            test("switches local mode off when nothing is published") {
                val mode = resolveLocalMode(inputs(ciVariable = "CI_PIPELINE_ID"))

                mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "CI_PIPELINE_ID"
            }

            test("fails the build when the runner carries local state") {
                val failure = shouldThrow<GradleException> {
                    resolveLocalMode(inputs(workspace("core", "net"), ciVariable = "GITHUB_ACTIONS"))
                }

                failure.message.orEmpty() shouldContain "while running in CI"
                failure.message.orEmpty() shouldContain "GITHUB_ACTIONS"
                failure.message.orEmpty() shouldContain "core"
            }
        }

        context("an empty state directory") {

            test("switches local mode off without complaining") {
                val mode = resolveLocalMode(inputs())

                mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "nothing is published"
            }

            test("fails when local mode was demanded explicitly") {
                val failure = shouldThrow<GradleException> {
                    resolveLocalMode(inputs(requested = true))
                }

                failure.message.orEmpty() shouldContain "kreateLocalPublish"
            }

            test("names the narrowing filter when one excluded everything") {
                val failure = shouldThrow<GradleException> {
                    resolveLocalMode(inputs(workspace("core"), requested = true, only = setOf("json")))
                }

                failure.message.orEmpty() shouldContain "kreate.local.only=json"
            }
        }

        context("an available workspace") {

            test("switches local mode on without anyone asking for it") {
                val mode = resolveLocalMode(inputs(workspace("core")))

                mode.shouldBeInstanceOf<LocalMode.Active>().workspace.libraries.map { it.library } shouldBe
                    listOf("core")
            }

            test("is narrowed by the only filter") {
                val mode = resolveLocalMode(inputs(workspace("core", "net"), only = setOf("net")))

                mode.workspace.libraries.map { it.library } shouldBe listOf("net")
            }
        }

        context("parsing the switch") {

            test("accepts true and false in any case, and treats blank as unset") {
                parseLocalRequest("TRUE") shouldBe true
                parseLocalRequest("False") shouldBe false
                parseLocalRequest("  ") shouldBe null
                parseLocalRequest(null) shouldBe null
            }

            test("rejects anything else rather than silently meaning false") {
                val failure = shouldThrow<GradleException> { parseLocalRequest("yes") }

                failure.message.orEmpty() shouldContain "'true' or 'false'"
            }

            test("splits the only filter and drops empty entries") {
                parseLocalOnly(" core , net ,, ") shouldBe setOf("core", "net")
                parseLocalOnly(null) shouldBe emptySet()
            }
        }
    }

    context("the mode accessors") {

        test("expose the workspace of an active mode") {
            val active: LocalMode = LocalMode.Active(workspace("core"))

            active.isActive shouldBe true
            active.workspace shouldBe workspace("core")
        }

        test("expose an empty workspace for an inactive mode") {
            val inactive: LocalMode = LocalMode.Inactive("nothing is published")

            inactive.isActive shouldBe false
            inactive.workspace shouldBe LocalWorkspace.EMPTY
        }
    }
})
