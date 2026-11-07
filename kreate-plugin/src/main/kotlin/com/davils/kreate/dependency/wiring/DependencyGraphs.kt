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
package com.davils.kreate.dependency.wiring

import com.davils.kreate.dependency.graph.DependencyGraphModel
import com.davils.kreate.dependency.graph.GraphEdge
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

internal fun dependencyGraphOf(name: String, root: ResolvedComponentResult): DependencyGraphModel {
    val edges = linkedSetOf<GraphEdge>()
    val visited = mutableSetOf<ResolvedComponentResult>()
    val pending = ArrayDeque(listOf(root))
    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!visited.add(component)) continue
        val resolved = component.dependencies.filterIsInstance<ResolvedDependencyResult>()
        val selected = resolved.map { result -> result.selected }
        selected.forEach { child -> edges += GraphEdge(idOf(component.id), idOf(child.id)) }
        pending += selected
    }
    return DependencyGraphModel(name, idOf(root.id), edges.filterNot { edge -> edge.from == edge.to })
}

internal fun idOf(identifier: ComponentIdentifier): String {
    if (identifier is ModuleComponentIdentifier) return "${identifier.group}:${identifier.module}:${identifier.version}"
    if (identifier is ProjectComponentIdentifier) return identifier.buildTreePath
    return identifier.displayName
}
