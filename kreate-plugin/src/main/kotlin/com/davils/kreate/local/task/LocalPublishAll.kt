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

package com.davils.kreate.local.task

import com.davils.kreate.host.HostPlatform
import com.davils.kreate.host.OperatingSystem
import com.davils.kreate.local.LocalWorkflowTaskNames
import com.davils.kreate.local.workspace.WorkspaceLibrary
import com.davils.kreate.local.workspace.downstreamOf
import com.davils.kreate.local.workspace.topologicalOrder
import com.davils.kreate.settings.local.LOCAL_PUBLISH_PROPERTY
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
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

@DisableCachingByDefault(because = "Drives other builds, whose inputs this build cannot see.")
internal abstract class LocalPublishAll @Inject constructor(
    private val execOperations: ExecOperations
) : KreateTask(
    "Publishes every declared repository to the local Maven repository, in dependency order.",
    KreateTaskGroup.LOCAL
) {
    @get:Input
    public abstract val libraries: ListProperty<WorkspaceLibrary>

    @get:Internal
    @get:Option(
        option = "from",
        description = "Publish these libraries and everything that depends on them."
    )
    public abstract val from: ListProperty<String>

    @get:Internal
    @get:Option(
        option = "only",
        description = "Publish exactly these libraries, without anything downstream."
    )
    public abstract val only: ListProperty<String>

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
        logger.lifecycle("Run ./gradlew ${LocalWorkflowTaskNames.STATUS} to see what consumers will resolve.")
    }

    private fun select(): List<WorkspaceLibrary> {
        val declared = libraries.get()
        val fromNames = names(from.getOrElse(emptyList()))
        val onlyNames = names(only.getOrElse(emptyList()))
        verifyNotCombined(fromNames, onlyNames)

        if (fromNames.isNotEmpty()) return downstreamOf(declared, fromNames)
        if (onlyNames.isNotEmpty()) return exactly(declared, onlyNames)
        return topologicalOrder(declared)
    }

    private fun verifyNotCombined(fromNames: Set<String>, onlyNames: Set<String>) {
        val isCombined = fromNames.isNotEmpty() && onlyNames.isNotEmpty()
        if (!isCombined) return

        throw GradleException(
            "--from and --only mean different things and cannot be combined. --from " +
                "republishes everything downstream of a change; --only republishes exactly " +
                "what is named."
        )
    }

    private fun exactly(declared: List<WorkspaceLibrary>, onlyNames: Set<String>): List<WorkspaceLibrary> {
        val ordered = topologicalOrder(declared)
        val selected = ordered.filter { it.name in onlyNames }
        verifyAllFound(onlyNames, selected)
        return selected
    }

    private fun names(values: List<String>): Set<String> {
        val trimmed = values
            .flatMap { value -> value.split(NAME_SEPARATOR) }
            .map { value -> value.trim() }
        return trimmed
            .filter { value -> value.isNotEmpty() }
            .toSet()
    }

    private fun verifyAllFound(requested: Set<String>, selected: List<WorkspaceLibrary>) {
        val found = selected.map { it.name }.toSet()
        val missing = requested.filterNot { it in found }.sorted()
        if (missing.isEmpty()) return

        val declaredNames = libraries.get().map { it.name }
        throw GradleException(
            "The workspace does not declare ${missing.joinToString()}. " +
                "It declares: ${declaredNames.sorted().joinToString()}."
        )
    }

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
            isIgnoreExitValue = true
        }

        if (result.exitValue == SUCCESS_EXIT_CODE) return

        val remaining = selected.drop(index + 1).map { it.name }
        throw publishFailure(library, result.exitValue, directory, remaining)
    }

    private fun publishFailure(
        library: WorkspaceLibrary,
        exitValue: Int,
        directory: File,
        remaining: List<String>
    ): GradleException = GradleException(
        buildString {
            appendLine("Publishing '${library.name}' failed (exit $exitValue).")
            appendLine()
            appendLine("    Repository: ${directory.absolutePath}")
            appendLine("    Tasks:      ${library.tasks.joinToString(" ")}")
            if (remaining.isNotEmpty()) appendLine("    Not run:    ${remaining.joinToString()}")
            appendLine()
            append(
                "The libraries published before this one are still installed; " +
                    "the workspace is half updated until this is fixed and rerun."
            )
        }
    )

    private fun wrapperIn(directory: File): File? {
        val candidates = WRAPPER_SCRIPTS.map { name -> File(directory, name) }
        val isWindows = HostPlatform.current().operatingSystemOrNull() == OperatingSystem.WINDOWS
        return candidates.firstOrNull { candidate ->
            candidate.isFile && candidate.name.endsWith(BATCH_EXTENSION) == isWindows
        }
    }

    private companion object {
        private const val SUCCESS_EXIT_CODE: Int = 0

        private const val NAME_SEPARATOR: Char = ','

        private const val BATCH_EXTENSION: String = ".bat"

        private val WRAPPER_SCRIPTS: List<String> = listOf("gradlew.bat", "gradlew")
    }
}
