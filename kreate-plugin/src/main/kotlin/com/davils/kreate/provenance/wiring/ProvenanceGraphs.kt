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
package com.davils.kreate.provenance.wiring

import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.dependency.wiring.resolvedGraphOf
import com.davils.kreate.provenance.marker.PluginMarker
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider

private const val CLASSPATH_CONFIGURATION: String = "classpath"

private const val MARKER_SUFFIX: String = ".gradle.plugin"

internal fun Project.provenanceGraphs(names: Set<String>, includeBuildscript: Boolean): ListProperty<ResolvedGraph> {
    val selected = configurations.filter { configuration -> configuration.name in names }
    val resolvable = selected.filter { configuration -> configuration.isCanBeResolved }
    val merged = objects.listProperty(ResolvedGraph::class.java)
    resolvable.forEach { configuration -> merged.add(graphOf(configuration)) }
    if (includeBuildscript) buildscriptClasspath()?.let { classpath -> merged.add(graphOf(classpath)) }
    return merged
}

internal fun Project.pluginMarkers(includeBuildscript: Boolean): Provider<List<PluginMarker>> {
    val classpath = buildscriptClasspath()
    if (!includeBuildscript || classpath == null) return provider { emptyList() }
    return classpath.incoming.resolutionResult.rootComponent.map(::pluginMarkersOf)
}

private fun Project.buildscriptClasspath(): Configuration? =
    buildscript.configurations.findByName(CLASSPATH_CONFIGURATION)

private fun graphOf(configuration: Configuration): Provider<ResolvedGraph> =
    configuration.incoming.resolutionResult.rootComponent.map(::resolvedGraphOf)

private fun pluginMarkersOf(root: ResolvedComponentResult): List<PluginMarker> {
    val visited = linkedSetOf<ResolvedComponentResult>()
    val pending = ArrayDeque(listOf(root))
    while (pending.isNotEmpty()) {
        val component = pending.removeFirst()
        if (!visited.add(component)) continue
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
            pending.add(dependency.selected)
        }
    }
    return visited.mapNotNull(::markerOf)
}

private fun markerOf(component: ResolvedComponentResult): PluginMarker? {
    val identifier = component.id as? ModuleComponentIdentifier ?: return null
    val pluginId = identifier.module.removeSuffix(MARKER_SUFFIX)
    if (pluginId == identifier.module || pluginId != identifier.group) return null

    val selected = component.dependencies.filterIsInstance<ResolvedDependencyResult>()
    val implementations = selected.mapNotNull { dependency -> coordinateOf(dependency.selected) }
    return PluginMarker(pluginId, coordinateOf(component) ?: return null, implementations)
}

private fun coordinateOf(component: ResolvedComponentResult): ModuleCoordinate? {
    val identifier = component.id as? ModuleComponentIdentifier ?: return null
    return ModuleCoordinate(identifier.group, identifier.module, identifier.version)
}
