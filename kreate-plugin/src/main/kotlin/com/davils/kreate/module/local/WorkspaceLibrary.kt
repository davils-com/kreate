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

import org.gradle.api.GradleException
import java.io.Serializable

/**
 * One repository in a declared workspace, flattened for the task that publishes it.
 *
 * Public because it is the element type of a task input, and Gradle has to be able to see it.
 * Consumers declare workspaces through
 * [com.davils.kreate.module.local.extension.LocalWorkspaceExtension], not by building these.
 *
 * @since 3.2.0
 */
public data class WorkspaceLibrary(
    /**
     * The library's name, which has to match the root project name of its build.
     * @since 3.2.0
     */
    val name: String,
    /**
     * The absolute path of the repository.
     * @since 3.2.0
     */
    val path: String,
    /**
     * The libraries that have to be published before this one.
     * @since 3.2.0
     */
    val dependencies: List<String>,
    /**
     * The tasks run in this repository to publish it locally.
     * @since 3.2.0
     */
    val tasks: List<String>
) : Serializable {
    private companion object {
        /**
         * The serial version identifier.
         * @since 3.2.0
         */
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Orders the declared libraries so that every one is published after everything it depends on.
 *
 * Publishing `leaf` before `arc` is not an error anyone sees: `leaf` resolves the *released* `arc`,
 * succeeds, and is installed as a build that was never compiled against the change being tested.
 * The order is therefore not a convenience — getting it wrong produces a confident wrong answer.
 *
 * @param libraries The declared libraries.
 * @return The libraries in dependency order.
 * @throws GradleException If a dependency is undeclared or the graph contains a cycle.
 * @since 3.2.0
 */
internal fun topologicalOrder(libraries: List<WorkspaceLibrary>): List<WorkspaceLibrary> {
    val byName = libraries.associateBy { it.name }
    verifyDeclared(libraries, byName.keys)

    val ordered = mutableListOf<WorkspaceLibrary>()
    val settled = mutableSetOf<String>()
    val visiting = linkedSetOf<String>()

    fun visit(library: WorkspaceLibrary) {
        if (library.name in settled) return
        if (!visiting.add(library.name)) throw cycleFailure(visiting.toList(), library.name)

        library.dependencies.sorted().forEach { dependency -> visit(byName.getValue(dependency)) }

        visiting.remove(library.name)
        settled.add(library.name)
        ordered.add(library)
    }

    // Sorted rather than declaration order, so that the same workspace always publishes in the
    // same sequence and a failing run can be compared against a previous one.
    libraries.sortedBy { it.name }.forEach(::visit)

    return ordered
}

/**
 * Returns the libraries that have to be republished when the named ones change.
 *
 * This is what `--from arc` means: `arc` itself, plus everything that depends on it directly or
 * transitively. Anything else in the workspace is untouched, because republishing it would only
 * cost time and overwrite a snapshot someone may be relying on.
 *
 * @param libraries The declared libraries.
 * @param roots The names the change starts from.
 * @return The affected libraries, in dependency order.
 * @throws GradleException If a root is not declared, or the graph is not orderable.
 * @since 3.2.0
 */
internal fun downstreamOf(
    libraries: List<WorkspaceLibrary>,
    roots: Set<String>
): List<WorkspaceLibrary> {
    val ordered = topologicalOrder(libraries)
    verifyKnown(roots, ordered.map { it.name }.toSet())

    val affected = roots.toMutableSet()
    // One pass over the topological order suffices: a library's dependencies always precede it,
    // so by the time it is examined every dependency's membership is already final.
    ordered.forEach { library ->
        if (library.dependencies.any { dependency -> dependency in affected }) {
            affected.add(library.name)
        }
    }

    return ordered.filter { it.name in affected }
}

/**
 * Fails when a library depends on something the workspace does not declare.
 *
 * @param libraries The declared libraries.
 * @param declared The declared names.
 * @throws GradleException If a dependency is undeclared.
 * @since 3.2.0
 */
private fun verifyDeclared(libraries: List<WorkspaceLibrary>, declared: Set<String>) {
    val missing = libraries
        .flatMap { library -> library.dependencies.map { library.name to it } }
        .filter { (_, dependency) -> dependency !in declared }

    if (missing.isEmpty()) return

    throw GradleException(
        buildString {
            appendLine("The workspace declares dependencies on libraries it does not contain:")
            appendLine()
            missing.sortedBy { it.second }.forEach { (library, dependency) ->
                appendLine("    $library depends on '$dependency'")
            }
            appendLine()
            append("Declare them with library(\"<name>\") { path = \"...\" }, or remove the edge.")
        }
    )
}

/**
 * Fails when a requested root is not part of the workspace.
 *
 * @param roots The requested names.
 * @param declared The declared names.
 * @throws GradleException If a root is undeclared.
 * @since 3.2.0
 */
private fun verifyKnown(roots: Set<String>, declared: Set<String>) {
    val unknown = roots.filterNot { it in declared }.sorted()
    if (unknown.isEmpty()) return

    throw GradleException(
        "The workspace does not declare ${unknown.joinToString()}. " +
            "It declares: ${declared.sorted().joinToString()}."
    )
}

/**
 * Builds the failure for a dependency cycle.
 *
 * The cycle is rendered as the path that produced it rather than reported as the bare fact that
 * one exists. A workspace has a dozen entries and a dozen edges; "cycle detected" leaves the
 * reader to find it by hand, and the path is already known at the point of detection.
 *
 * @param path The libraries currently being visited, in order.
 * @param repeated The library that closed the cycle.
 * @return The failure to throw.
 * @since 3.2.0
 */
private fun cycleFailure(path: List<String>, repeated: String): GradleException {
    val cycle = path.dropWhile { it != repeated } + repeated

    return GradleException(
        "The workspace has a dependency cycle: ${cycle.joinToString(" -> ")}. " +
            "Remove one of those edges — a local publish has to run in dependency order, and a " +
            "cycle has none."
    )
}
