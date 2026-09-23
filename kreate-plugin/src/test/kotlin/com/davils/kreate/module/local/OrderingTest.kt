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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the order a workspace is published in.
 *
 * Getting this wrong is not a visible failure. Publishing `leaf` before `arc` succeeds, and
 * installs a `leaf` compiled against the *released* `arc` — a confident wrong answer, which is the
 * expensive kind. Every ordering property is therefore asserted rather than assumed.
 */
@DisplayName("workspace ordering")
class OrderingTest {

    /**
     * The real Davils graph as of 3.2.0, which is the shape the feature has to handle.
     */
    private val davils = listOf(
        library("arc"),
        library("rise", "arc"),
        library("leaf", "arc", "rise"),
        library("sira", "arc", "rise", "leaf"),
        library("novy", "sira"),
        library("fexo", "sira"),
        library("shield")
    )

    private fun library(name: String, vararg dependencies: String) = WorkspaceLibrary(
        name = name,
        path = "/workspace/$name",
        dependencies = dependencies.toList(),
        tasks = listOf("kreateLocalPublish")
    )

    @Nested
    @DisplayName("topological order")
    inner class Topological {

        @Test
        @DisplayName("puts every library after everything it depends on")
        fun dependenciesComeFirst() {
            val ordered = topologicalOrder(davils).map { it.name }

            ordered.indexOf("arc") shouldBe 0
            ordered.indexOf("rise") shouldBe ordered.indexOf("arc") + 1
            (ordered.indexOf("leaf") > ordered.indexOf("rise")) shouldBe true
            (ordered.indexOf("sira") > ordered.indexOf("leaf")) shouldBe true
            (ordered.indexOf("novy") > ordered.indexOf("sira")) shouldBe true
            (ordered.indexOf("fexo") > ordered.indexOf("sira")) shouldBe true
        }

        @Test
        @DisplayName("includes a library nothing depends on and that depends on nothing")
        fun isolatedLibrary() {
            topologicalOrder(davils).map { it.name } shouldContain "shield"
        }

        @Test
        @DisplayName("is stable, so two runs of the same workspace can be compared")
        fun stable() {
            val first = topologicalOrder(davils).map { it.name }
            val second = topologicalOrder(davils.reversed()).map { it.name }

            second shouldBe first
        }

        @Test
        @DisplayName("handles an empty workspace")
        fun empty() {
            topologicalOrder(emptyList()) shouldBe emptyList()
        }
    }

    @Nested
    @DisplayName("downstream selection")
    inner class Downstream {

        @Test
        @DisplayName("includes the root and everything that transitively depends on it")
        fun fansOut() {
            val selected = downstreamOf(davils, setOf("rise")).map { it.name }

            selected shouldBe listOf("rise", "leaf", "sira", "fexo", "novy")
        }

        @Test
        @DisplayName("leaves upstream libraries alone")
        fun doesNotIncludeUpstream() {
            val selected = downstreamOf(davils, setOf("leaf")).map { it.name }

            // Republishing `arc` would cost time and overwrite a snapshot someone may be relying
            // on, and nothing about a change in `leaf` makes it necessary.
            (selected.contains("arc") || selected.contains("rise")) shouldBe false
        }

        @Test
        @DisplayName("selects only the root when nothing depends on it")
        fun leafLibrary() {
            downstreamOf(davils, setOf("novy")).map { it.name } shouldBe listOf("novy")
        }

        @Test
        @DisplayName("selects the whole graph from its root")
        fun fromTheRoot() {
            downstreamOf(davils, setOf("arc")).map { it.name }.size shouldBe davils.size - 1
        }
    }

    @Nested
    @DisplayName("failures")
    inner class Failures {

        @Test
        @DisplayName("renders a cycle as the path that produced it")
        fun cycle() {
            val cyclic = listOf(
                library("a", "c"),
                library("b", "a"),
                library("c", "b")
            )

            val failure = shouldThrow<GradleException> { topologicalOrder(cyclic) }

            // "cycle detected" leaves the reader to find it across a dozen entries by hand.
            failure.message.orEmpty() shouldContain "a -> c -> b -> a"
        }

        @Test
        @DisplayName("names an edge pointing at a library the workspace does not declare")
        fun undeclaredDependency() {
            val incomplete = listOf(library("rise", "arc"))

            val failure = shouldThrow<GradleException> { topologicalOrder(incomplete) }

            failure.message.orEmpty() shouldContain "rise depends on 'arc'"
        }

        @Test
        @DisplayName("names an unknown root, and lists what is available")
        fun unknownRoot() {
            val failure = shouldThrow<GradleException> { downstreamOf(davils, setOf("mica")) }

            failure.message.orEmpty() shouldContain "does not declare mica"
            failure.message.orEmpty() shouldContain "arc"
        }
    }
}
