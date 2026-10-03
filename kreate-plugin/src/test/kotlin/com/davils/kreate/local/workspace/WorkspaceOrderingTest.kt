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

package com.davils.kreate.local.workspace

import com.davils.kreate.local.LocalWorkflowTaskNames
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class WorkspaceOrderingTest : FunSpec({
    fun library(name: String, vararg dependencies: String) = WorkspaceLibrary(
        name = name,
        path = "/workspace/$name",
        dependencies = dependencies.toList(),
        tasks = listOf(LocalWorkflowTaskNames.PUBLISH)
    )

    val graph = listOf(
        library("core"),
        library("net", "core"),
        library("json", "core", "net"),
        library("http", "core", "net", "json"),
        library("ui", "http"),
        library("cli", "http"),
        library("standalone")
    )

    context("workspace ordering") {
        context("topological order") {
            test("puts every library after everything it depends on") {
                val ordered = topologicalOrder(graph).map { it.name }

                ordered.indexOf("core") shouldBe 0
                ordered.indexOf("net") shouldBeGreaterThan ordered.indexOf("core")
                ordered.indexOf("json") shouldBeGreaterThan ordered.indexOf("net")
                ordered.indexOf("http") shouldBeGreaterThan ordered.indexOf("json")
                ordered.indexOf("ui") shouldBeGreaterThan ordered.indexOf("http")
                ordered.indexOf("cli") shouldBeGreaterThan ordered.indexOf("http")
            }

            test("includes a library nothing depends on and that depends on nothing") {
                topologicalOrder(graph).map { it.name } shouldContain "standalone"
            }

            test("is stable, so two runs of the same workspace can be compared") {
                val first = topologicalOrder(graph).map { it.name }
                val second = topologicalOrder(graph.reversed()).map { it.name }

                second shouldBe first
            }

            test("handles an empty workspace") {
                topologicalOrder(emptyList()) shouldBe emptyList()
            }
        }

        context("downstream selection") {
            test("includes the root and everything that transitively depends on it") {
                val selected = downstreamOf(graph, setOf("net")).map { it.name }

                selected shouldBe listOf("net", "json", "http", "cli", "ui")
            }

            test("leaves upstream libraries alone") {
                val selected = downstreamOf(graph, setOf("json")).map { it.name }

                selected shouldNotContain "core"
                selected shouldNotContain "net"
            }

            test("selects only the root when nothing depends on it") {
                downstreamOf(graph, setOf("ui")).map { it.name } shouldBe listOf("ui")
            }

            test("selects the whole graph from its root") {
                downstreamOf(graph, setOf("core")) shouldHaveSize graph.size - 1
            }
        }

        context("failures") {
            test("renders a cycle as the path that produced it") {
                val cyclic = listOf(
                    library("a", "c"),
                    library("b", "a"),
                    library("c", "b")
                )

                val failure = shouldThrow<IllegalArgumentException> { topologicalOrder(cyclic) }

                failure.message.orEmpty() shouldContain "a -> c -> b -> a"
            }

            test("names an edge pointing at a library the workspace does not declare") {
                val incomplete = listOf(library("net", "core"))

                val failure = shouldThrow<IllegalArgumentException> { topologicalOrder(incomplete) }

                failure.message.orEmpty() shouldContain "net depends on 'core'"
            }

            test("names an unknown root, and lists what is available") {
                val failure = shouldThrow<IllegalArgumentException> { downstreamOf(graph, setOf("absent")) }
                val message = failure.message.orEmpty()

                message shouldContain "does not declare absent"
                message shouldContain "core"
            }
        }
    }
})
