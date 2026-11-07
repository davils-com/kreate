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
package com.davils.kreate.dependency.graph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val MODEL: DependencyGraphModel = DependencyGraphModel(
    "runtimeClasspath",
    ":app",
    listOf(GraphEdge(":app", "a:b:1"), GraphEdge(":app", ":core"), GraphEdge(":core", "a:b:1"))
)

class GraphRendererTest : FunSpec({
    context("a graph") {
        test("renders as a tree that marks repeated nodes") {
            GraphRenderer.render(listOf(MODEL), GraphFormat.TEXT) shouldBe
                "runtimeClasspath:\n+--- :core\n|   +--- a:b:1\n+--- a:b:1 (*)\n"
        }

        test("renders as DOT with quoted nodes") {
            GraphRenderer.render(listOf(MODEL), GraphFormat.DOT) shouldContain "    \":core\" -> \"a:b:1\";"
        }

        test("renders as Mermaid with stable ids") {
            val text = GraphRenderer.render(listOf(MODEL), GraphFormat.MERMAID)

            text shouldContain "graph LR"
            text shouldContain "n0[\":app\"]"
            text shouldContain "n0 --> n1"
        }

        test("knows its formats") {
            GraphFormat.parse("Mermaid") shouldBe GraphFormat.MERMAID
            GraphFormat.parse("svg") shouldBe null
        }
    }
})
