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

private const val INDENT: String = "    "

private const val BRANCH: String = "|   "

internal object GraphRenderer {
    fun render(models: List<DependencyGraphModel>, format: GraphFormat): String = when (format) {
        GraphFormat.TEXT -> models.joinToString("\n") { model -> textOf(model) }
        GraphFormat.DOT -> dotOf(models)
        GraphFormat.MERMAID -> mermaidOf(models)
    }

    private fun textOf(model: DependencyGraphModel): String {
        val children = model.edges.groupBy({ edge -> edge.from }, { edge -> edge.to })
        val lines = mutableListOf("${model.name}:")
        visit(model.root, 0, children, mutableSetOf(), lines)
        return lines.joinToString("\n", postfix = "\n")
    }

    private fun visit(
        node: String,
        depth: Int,
        children: Map<String, List<String>>,
        seen: MutableSet<String>,
        lines: MutableList<String>
    ) {
        val sorted = children[node].orEmpty().sorted()
        sorted.forEach { child ->
            val repeated = !seen.add(child)
            val mark = " (*)".takeIf { repeated }.orEmpty()
            lines += "${BRANCH.repeat(depth)}+--- $child$mark"
            if (!repeated) visit(child, depth + 1, children, seen, lines)
        }
    }

    private fun dotOf(models: List<DependencyGraphModel>): String {
        val lines = edgesOf(models).map { edge -> "$INDENT${quoted(edge.from)} -> ${quoted(edge.to)};" }
        return (listOf("digraph dependencies {") + lines + "}").joinToString("\n", postfix = "\n")
    }

    private fun mermaidOf(models: List<DependencyGraphModel>): String {
        val all = models.flatMap { model -> model.nodes }
        val nodes = all.distinct().sorted()
        val ids = nodes.withIndex().associate { (index, node) -> node to "n$index" }
        val declarations = nodes.map { node -> "$INDENT${ids[node]}[\"${node.replace("\"", "'")}\"]" }
        val links = edgesOf(models).map { edge -> "$INDENT${ids[edge.from]} --> ${ids[edge.to]}" }
        return (listOf("graph LR") + declarations + links).joinToString("\n", postfix = "\n")
    }

    private fun edgesOf(models: List<DependencyGraphModel>): List<GraphEdge> {
        val edges = models.flatMap { model -> model.edges }
        return edges.distinct().sortedBy { edge -> edge.from + edge.to }
    }

    private fun quoted(node: String): String {
        val escaped = node.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
