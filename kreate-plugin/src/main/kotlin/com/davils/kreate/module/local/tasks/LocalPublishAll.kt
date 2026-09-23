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

package com.davils.kreate.module.local.tasks

import com.davils.kreate.KreateTasks
import com.davils.kreate.jobs.Task
import com.davils.kreate.module.local.LOCAL_PUBLISH_PROPERTY
import com.davils.kreate.module.local.WorkspaceLibrary
import com.davils.kreate.module.local.downstreamOf
import com.davils.kreate.module.local.topologicalOrder
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Publishes a whole workspace of repositories locally, in dependency order.
 *
 * A change at the root of the Davils dependency graph has to be pushed through every library
 * above it before it can be tried in the one that matters. Done by hand that means remembering
 * both the set and the order, and getting the order wrong does not produce an error — it produces
 * a library published against the *released* version of the thing that just changed.
 *
 * Each repository is built by its own wrapper in its own process. Sharing a daemon across a dozen
 * unrelated builds would mean one JVM holding every plugin classpath in the workspace at once,
 * and a failure in one repository taking the rest with it.
 *
 * @param execOperations The service used to run each repository's wrapper.
 * @since 3.2.0
 */
@DisableCachingByDefault(because = "Drives other builds, whose inputs this build cannot see.")
public abstract class LocalPublishAll @Inject constructor(
    private val execOperations: ExecOperations
) : Task(
    "Publishes every declared repository to the local Maven repository, in dependency order.",
    KreateTasks.Local.GROUP
) {
    /**
     * The declared workspace, flattened at configuration time.
     * @since 3.2.0
     */
    @get:Input
    public abstract val libraries: ListProperty<WorkspaceLibrary>

    /**
     * Publishes only these libraries and everything downstream of them.
     * @since 3.2.0
     */
    @get:Internal
    @get:Option(
        option = "from",
        description = "Publish these libraries and everything that depends on them."
    )
    public abstract val from: ListProperty<String>

    /**
     * Publishes only these libraries, ignoring what depends on them.
     * @since 3.2.0
     */
    @get:Internal
    @get:Option(
        option = "only",
        description = "Publish exactly these libraries, without anything downstream."
    )
    public abstract val only: ListProperty<String>

    /**
     * Whether the build is running on Windows, which uses a different wrapper script.
     *
     * @since 3.2.0
     */
    private val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /**
     * Runs a local publish in every selected repository.
     *
     * @since 3.2.0
     */
    @TaskAction
    public fun publishAll() {
        val selected = select()
        if (selected.isEmpty()) {
            logger.lifecycle("Nothing selected. The workspace declares no libraries.")
            return
        }

        logger.lifecycle("Publishing ${selected.size} repositories in order:")
        selected.forEachIndexed { index, library ->
            logger.lifecycle("  ${index + 1}. ${library.name}")
        }
        logger.lifecycle("")

        selected.forEachIndexed { index, library -> publish(library, index, selected) }

        logger.lifecycle("")
        logger.lifecycle("Published ${selected.size} repositories: ${selected.joinToString { it.name }}.")
        logger.lifecycle("Run ./gradlew ${KreateTasks.Local.STATUS} to see what consumers will resolve.")
    }

    /**
     * Resolves the selection from the command line options.
     *
     * @return The libraries to publish, in dependency order.
     * @throws GradleException If both options were given, or a named library is undeclared.
     * @since 3.2.0
     */
    private fun select(): List<WorkspaceLibrary> {
        val declared = libraries.get()
        val fromNames = names(from.getOrElse(emptyList()))
        val onlyNames = names(only.getOrElse(emptyList()))

        if (fromNames.isNotEmpty() && onlyNames.isNotEmpty()) {
            throw GradleException(
                "--from and --only mean different things and cannot be combined. --from " +
                    "republishes everything downstream of a change; --only republishes exactly " +
                    "what is named."
            )
        }

        return when {
            fromNames.isNotEmpty() -> downstreamOf(declared, fromNames)
            onlyNames.isNotEmpty() -> topologicalOrder(declared).filter { it.name in onlyNames }
                .also { selected -> verifyAllFound(onlyNames, selected) }
            else -> topologicalOrder(declared)
        }
    }

    /**
     * Splits an option's values, which Gradle accepts both repeated and comma separated.
     *
     * @param values The raw option values.
     * @return The named libraries.
     * @since 3.2.0
     */
    private fun names(values: List<String>): Set<String> = values
        .flatMap { value -> value.split(',') }
        .map { value -> value.trim() }
        .filter { value -> value.isNotEmpty() }
        .toSet()

    /**
     * Fails when `--only` named something the workspace does not declare.
     *
     * @param requested The requested names.
     * @param selected The libraries that matched.
     * @throws GradleException If a name matched nothing.
     * @since 3.2.0
     */
    private fun verifyAllFound(requested: Set<String>, selected: List<WorkspaceLibrary>) {
        val found = selected.map { it.name }.toSet()
        val missing = requested.filterNot { it in found }.sorted()

        if (missing.isNotEmpty()) {
            throw GradleException(
                "The workspace does not declare ${missing.joinToString()}. " +
                    "It declares: ${libraries.get().map { it.name }.sorted().joinToString()}."
            )
        }
    }

    /**
     * Runs one repository's local publish.
     *
     * @param library The repository to publish.
     * @param index The position in the run, for the banner.
     * @param selected Everything being published, so a failure can name what was skipped.
     * @throws GradleException If the repository is missing or its build fails.
     * @since 3.2.0
     */
    private fun publish(library: WorkspaceLibrary, index: Int, selected: List<WorkspaceLibrary>) {
        val directory = File(library.path)
        val wrapper = wrapperIn(directory)
            ?: throw GradleException(
                "No Gradle wrapper in '${directory.absolutePath}' for library " +
                    "'${library.name}'. Check the path declared in the workspace."
            )

        logger.lifecycle("> ${library.name} (${index + 1}/${selected.size}) — ${directory.absolutePath}")

        val result = execOperations.exec {
            workingDir = directory
            commandLine(
                listOf(wrapper.absolutePath) + library.tasks + "-P$LOCAL_PUBLISH_PROPERTY=true"
            )
            // Handled below, so that the failure can name what did not run.
            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            val remaining = selected.drop(index + 1).map { it.name }
            throw GradleException(
                buildString {
                    appendLine("Publishing '${library.name}' failed (exit ${result.exitValue}).")
                    appendLine()
                    appendLine("    Repository: ${directory.absolutePath}")
                    appendLine("    Tasks:      ${library.tasks.joinToString(" ")}")
                    if (remaining.isNotEmpty()) {
                        appendLine("    Not run:    ${remaining.joinToString()}")
                    }
                    appendLine()
                    append(
                        "The libraries published before this one are still installed; " +
                            "the workspace is half updated until this is fixed and rerun."
                    )
                }
            )
        }
    }

    /**
     * Returns the Gradle wrapper of a repository.
     *
     * @param directory The repository directory.
     * @return The wrapper script, or `null` if there is none.
     * @since 3.2.0
     */
    private fun wrapperIn(directory: File): File? = listOf("gradlew.bat", "gradlew")
        .map { name -> File(directory, name) }
        .firstOrNull { candidate ->
            candidate.isFile && candidate.name.endsWith(".bat") == isWindows
        }
}
