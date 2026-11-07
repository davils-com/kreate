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
package com.davils.kreate.workspace.policy

internal class StronglyConnected private constructor(private val graph: Map<String, List<String>>) {
    private val index: MutableMap<String, Int> = mutableMapOf()

    private val lowLink: MutableMap<String, Int> = mutableMapOf()

    private val stack: ArrayDeque<String> = ArrayDeque()

    private val onStack: MutableSet<String> = mutableSetOf()

    private val components: MutableList<Set<String>> = mutableListOf()

    private var counter: Int = 0

    private fun visit(node: String) {
        index[node] = counter
        lowLink[node] = counter
        counter += 1
        stack.addLast(node)
        onStack += node
        graph[node].orEmpty().forEach { next -> follow(node, next) }
        if (lowLink[node] == index[node]) components += popComponent(node)
    }

    private fun visitOnce(node: String) {
        if (node !in index) visit(node)
    }

    private fun follow(node: String, next: String) {
        if (next !in index) {
            visit(next)
            lowLink[node] = minOf(lowLink.getValue(node), lowLink.getValue(next))
        }
        if (next in onStack) lowLink[node] = minOf(lowLink.getValue(node), index.getValue(next))
    }

    private fun popComponent(root: String): Set<String> {
        val component = mutableSetOf<String>()
        var node: String
        do {
            node = stack.removeLast()
            onStack -= node
            component += node
        } while (node != root)
        return component
    }

    companion object {
        fun componentsOf(graph: Map<String, List<String>>): List<Set<String>> {
            val search = StronglyConnected(graph)
            graph.keys.sorted().forEach { node -> search.visitOnce(node) }
            return search.components
        }
    }
}
