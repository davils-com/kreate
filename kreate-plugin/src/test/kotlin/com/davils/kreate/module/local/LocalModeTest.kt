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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.GradleException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the rule that decides whether a build resolves from the local Maven repository.
 *
 * Every branch is covered deliberately. This rule is the difference between a developer's
 * convenience and a pipeline publishing an artefact built against a dependency that exists on one
 * machine, so "it works when I tried it" is not an adequate level of confidence.
 */
@DisplayName("resolveLocalMode")
class LocalModeTest {

    private fun workspace(vararg libraries: String): LocalWorkspace = LocalWorkspace(
        libraries.map { name ->
            LocalLibrary(
                library = name,
                group = "com.davils",
                repository = java.io.File("/workspace/$name"),
                version = "1.0.0-SNAPSHOT",
                publishedAt = null,
                kreateVersion = "3.2.0",
                modules = listOf(LocalModule("com.davils", name))
            )
        }
    )

    private fun inputs(
        workspace: LocalWorkspace = LocalWorkspace.EMPTY,
        requested: Boolean? = null,
        only: Set<String> = emptySet(),
        ciVariable: String? = null
    ) = LocalModeInputs(
        workspace = workspace,
        stateDirectory = "/home/dev/.gradle/davils/local",
        requested = requested,
        only = only,
        continuousIntegration = ciVariable != null,
        ciVariable = ciVariable
    )

    @Nested
    @DisplayName("the explicit off switch")
    inner class ExplicitlyOff {

        @Test
        @DisplayName("wins over everything, including a populated state directory")
        fun offWins() {
            val mode = resolveLocalMode(inputs(workspace("arc"), requested = false))

            mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "switched off"
        }

        @Test
        @DisplayName("wins over CI detection, so it never turns a refusal into a failure")
        fun offWinsOverCi() {
            val mode = resolveLocalMode(
                inputs(workspace("arc"), requested = false, ciVariable = "GITLAB_CI")
            )

            mode.isActive shouldBe false
        }
    }

    @Nested
    @DisplayName("continuous integration")
    inner class ContinuousIntegration {

        @Test
        @DisplayName("switches local mode off when nothing is published")
        fun offInCi() {
            val mode = resolveLocalMode(inputs(ciVariable = "CI_PIPELINE_ID"))

            mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "CI_PIPELINE_ID"
        }

        @Test
        @DisplayName("fails the build when the runner carries local state")
        fun failsInCiWithState() {
            val failure = shouldThrow<GradleException> {
                resolveLocalMode(inputs(workspace("arc", "rise"), ciVariable = "GITHUB_ACTIONS"))
            }

            failure.message.orEmpty() shouldContain "while running in CI"
            failure.message.orEmpty() shouldContain "GITHUB_ACTIONS"
            failure.message.orEmpty() shouldContain "arc"
        }
    }

    @Nested
    @DisplayName("an empty state directory")
    inner class NothingPublished {

        @Test
        @DisplayName("switches local mode off without complaining")
        fun quietlyOff() {
            val mode = resolveLocalMode(inputs())

            mode.shouldBeInstanceOf<LocalMode.Inactive>().reason shouldContain "nothing is published"
        }

        @Test
        @DisplayName("fails when local mode was demanded explicitly")
        fun failsWhenDemanded() {
            val failure = shouldThrow<GradleException> {
                resolveLocalMode(inputs(requested = true))
            }

            failure.message.orEmpty() shouldContain "kreateLocalPublish"
        }

        @Test
        @DisplayName("names the narrowing filter when one excluded everything")
        fun failureNamesTheFilter() {
            val failure = shouldThrow<GradleException> {
                resolveLocalMode(inputs(workspace("arc"), requested = true, only = setOf("leaf")))
            }

            failure.message.orEmpty() shouldContain "davils.local.only=leaf"
        }
    }

    @Nested
    @DisplayName("an available workspace")
    inner class Available {

        @Test
        @DisplayName("switches local mode on without anyone asking for it")
        fun activeWithoutRequest() {
            val mode = resolveLocalMode(inputs(workspace("arc")))

            mode.shouldBeInstanceOf<LocalMode.Active>().workspace.libraries.map { it.library } shouldBe
                listOf("arc")
        }

        @Test
        @DisplayName("is narrowed by the only filter")
        fun narrowed() {
            val mode = resolveLocalMode(inputs(workspace("arc", "rise"), only = setOf("rise")))

            mode.workspace.libraries.map { it.library } shouldBe listOf("rise")
        }
    }

    @Nested
    @DisplayName("parsing the switch")
    inner class Parsing {

        @Test
        @DisplayName("accepts true and false in any case, and treats blank as unset")
        fun accepted() {
            parseLocalRequest("TRUE") shouldBe true
            parseLocalRequest("False") shouldBe false
            parseLocalRequest("  ") shouldBe null
            parseLocalRequest(null) shouldBe null
        }

        @Test
        @DisplayName("rejects anything else rather than silently meaning false")
        fun rejectsTypos() {
            // A typo in a flag that switches dependency resolution looks exactly like the
            // feature not working, which is the most expensive way for it to fail.
            val failure = shouldThrow<GradleException> { parseLocalRequest("yes") }

            failure.message.orEmpty() shouldContain "'true' or 'false'"
        }

        @Test
        @DisplayName("splits the only filter and drops empty entries")
        fun onlyFilter() {
            parseLocalOnly(" arc , rise ,, ") shouldBe setOf("arc", "rise")
            parseLocalOnly(null) shouldBe emptySet()
        }
    }
}
