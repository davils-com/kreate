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

private const val CYCLE_SEPARATOR: String = " -> "

internal fun topologicalOrder(libraries: List<WorkspaceLibrary>): List<WorkspaceLibrary> {
    val byName = libraries.associateBy { it.name }
    verifyDeclared(libraries, byName.keys)

    val ordered = mutableListOf<WorkspaceLibrary>()
    val settled = mutableSetOf<String>()
    val visiting = linkedSetOf<String>()

    fun visit(library: WorkspaceLibrary) {
        if (library.name in settled) return
        val firstVisit = visiting.add(library.name)
        require(firstVisit) { cycleMessage(visiting.toList(), library.name) }

        val dependencies = library.dependencies.sorted()
        dependencies.forEach { dependency -> visit(byName.getValue(dependency)) }

        visiting.remove(library.name)
        settled.add(library.name)
        ordered.add(library)
    }

    val reproducibleVisitOrder = libraries.sortedBy { it.name }
    reproducibleVisitOrder.forEach(::visit)

    return ordered.toList()
}

internal fun downstreamOf(
    libraries: List<WorkspaceLibrary>,
    roots: Set<String>
): List<WorkspaceLibrary> {
    val dependenciesFirst = topologicalOrder(libraries)
    val declared = dependenciesFirst.map { it.name }.toSet()
    verifyKnown(roots, declared)

    val affected = roots.toMutableSet()
    dependenciesFirst.forEach { library ->
        if (library.dependsOnAnyOf(affected)) affected.add(library.name)
    }

    return dependenciesFirst.filter { it.name in affected }
}

private fun WorkspaceLibrary.dependsOnAnyOf(names: Set<String>): Boolean =
    dependencies.any { dependency -> dependency in names }

private fun verifyDeclared(libraries: List<WorkspaceLibrary>, declared: Set<String>) {
    val missing = libraries
        .flatMap { library -> library.dependencies.map { library.name to it } }
        .filter { (_, dependency) -> dependency !in declared }

    require(missing.isEmpty()) { undeclaredDependenciesMessage(missing) }
}

private fun undeclaredDependenciesMessage(missing: List<Pair<String, String>>): String = buildString {
    appendLine("The workspace declares dependencies on libraries it does not contain:")
    appendLine()
    val byDependency = missing.sortedBy { it.second }
    byDependency.forEach { (library, dependency) -> appendLine("    $library depends on '$dependency'") }
    appendLine()
    append("Declare them with library(\"<name>\") { path = \"...\" }, or remove the edge.")
}

private fun verifyKnown(roots: Set<String>, declared: Set<String>) {
    val unknown = roots.filterNot { it in declared }.sorted()
    require(unknown.isEmpty()) {
        "The workspace does not declare ${unknown.joinToString()}. " +
            "It declares: ${declared.sorted().joinToString()}."
    }
}

private fun cycleMessage(path: List<String>, repeated: String): String {
    val cycle = path.dropWhile { it != repeated } + repeated

    return "The workspace has a dependency cycle: ${cycle.joinToString(CYCLE_SEPARATOR)}. " +
        "Remove one of those edges — a local publish has to run in dependency order, and a " +
        "cycle has none."
}
