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
 * Getting this wrong is not a visible failure. Publishing a library before the one it depends on
 * succeeds, and installs a build compiled against the *released* version of the thing that just
 * changed — a confident wrong answer, which is the expensive kind. Every ordering property is
 * therefore asserted rather than assumed.
 */
@DisplayName("workspace ordering")
class OrderingTest {

    /**
     * A layered graph with a shared root, a fan-out and an unrelated library — between them the
     * shapes a real workspace produces.
     */
    private val graph = listOf(
        library("core"),
        library("net", "core"),
        library("json", "core", "net"),
        library("http", "core", "net", "json"),
        library("ui", "http"),
        library("cli", "http"),
        library("standalone")
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
            val ordered = topologicalOrder(graph).map { it.name }

            ordered.indexOf("core") shouldBe 0
            (ordered.indexOf("net") > ordered.indexOf("core")) shouldBe true
            (ordered.indexOf("json") > ordered.indexOf("net")) shouldBe true
            (ordered.indexOf("http") > ordered.indexOf("json")) shouldBe true
            (ordered.indexOf("ui") > ordered.indexOf("http")) shouldBe true
            (ordered.indexOf("cli") > ordered.indexOf("http")) shouldBe true
        }

        @Test
        @DisplayName("includes a library nothing depends on and that depends on nothing")
        fun isolatedLibrary() {
            topologicalOrder(graph).map { it.name } shouldContain "standalone"
        }

        @Test
        @DisplayName("is stable, so two runs of the same workspace can be compared")
        fun stable() {
            val first = topologicalOrder(graph).map { it.name }
            val second = topologicalOrder(graph.reversed()).map { it.name }

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
            val selected = downstreamOf(graph, setOf("net")).map { it.name }

            selected shouldBe listOf("net", "json", "http", "cli", "ui")
        }

        @Test
        @DisplayName("leaves upstream libraries alone")
        fun doesNotIncludeUpstream() {
            val selected = downstreamOf(graph, setOf("json")).map { it.name }

            // Republishing `core` would cost time and overwrite a snapshot someone may be relying
            // on, and nothing about a change in `json` makes it necessary.
            (selected.contains("core") || selected.contains("net")) shouldBe false
        }

        @Test
        @DisplayName("selects only the root when nothing depends on it")
        fun leafLibrary() {
            downstreamOf(graph, setOf("ui")).map { it.name } shouldBe listOf("ui")
        }

        @Test
        @DisplayName("selects the whole graph from its root")
        fun fromTheRoot() {
            downstreamOf(graph, setOf("core")).map { it.name }.size shouldBe graph.size - 1
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
            val incomplete = listOf(library("net", "core"))

            val failure = shouldThrow<GradleException> { topologicalOrder(incomplete) }

            failure.message.orEmpty() shouldContain "net depends on 'core'"
        }

        @Test
        @DisplayName("names an unknown root, and lists what is available")
        fun unknownRoot() {
            val failure = shouldThrow<GradleException> { downstreamOf(graph, setOf("absent")) }

            failure.message.orEmpty() shouldContain "does not declare absent"
            failure.message.orEmpty() shouldContain "core"
        }
    }
}
