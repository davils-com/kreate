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

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult

internal fun resolvedGraphOf(root: ResolvedComponentResult): ResolvedGraph {
    val visited = linkedSetOf<ResolvedComponentResult>()
    val unresolved = sortedSetOf<String>()
    val pending = ArrayDeque(listOf(root))

    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!visited.add(component)) continue

        component.dependencies.forEach { dependency ->
            if (dependency is ResolvedDependencyResult) pending.add(dependency.selected)
            if (dependency is UnresolvedDependencyResult) unresolved.add(dependency.requested.displayName)
        }
    }

    val modules = visited.mapNotNull { component -> coordinateOf(component) }
    return ResolvedGraph(modules.distinct(), unresolved.toList())
}

private fun coordinateOf(component: ResolvedComponentResult): ModuleCoordinate? {
    val identifier = component.id as? ModuleComponentIdentifier ?: return null
    return ModuleCoordinate(identifier.group, identifier.module, identifier.version)
}
