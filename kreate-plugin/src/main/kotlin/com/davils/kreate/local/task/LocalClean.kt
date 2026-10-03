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

import com.davils.kreate.local.LocalWorkflowTaskNames
import com.davils.kreate.settings.local.LocalWorkspace
import com.davils.kreate.settings.local.moduleDirectory
import com.davils.kreate.settings.local.readLocalWorkspace
import com.davils.kreate.settings.local.snapshotDirectories
import com.davils.kreate.settings.local.stateFileOf
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Deletes machine state outside the project directory.")
internal abstract class LocalClean : KreateTask(
    "Removes everything published to the local Maven repository by ${LocalWorkflowTaskNames.PUBLISH}.",
    KreateTaskGroup.LOCAL
) {
    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    @get:Internal
    public abstract val sweepAll: Property<Boolean>

    @get:Internal
    public abstract val continuousIntegration: Property<Boolean>

    @TaskAction
    public fun clean() {
        check(!continuousIntegration.get()) {
            "${LocalWorkflowTaskNames.CLEAN} deletes from the machine's Maven repository and must " +
                "not run in CI."
        }

        val state = stateDirectory.get().asFile
        val repository = mavenLocal.get().asFile
        val workspace = readLocalWorkspace(state)

        if (workspace.isEmpty) {
            logger.lifecycle("Nothing is published locally. Nothing to clean.")
            return
        }

        val recorded = deleteRecordedArtifacts(workspace, repository)
        val swept = sweepSnapshotsIfRequested(workspace, repository)
        val distinctRemoved = (recorded + swept).distinct()

        distinctRemoved.sorted().forEach { path -> logger.lifecycle("  removed $path") }
        deleteRecords(state, workspace)

        logger.lifecycle("")
        logger.lifecycle(
            "Removed ${distinctRemoved.size} ${directoryNoun(distinctRemoved.size)} " +
                "and ${workspace.libraries.size} record(s). Builds resolve released versions again."
        )
    }

    private fun sweepSnapshotsIfRequested(workspace: LocalWorkspace, repository: File): List<String> {
        if (!sweepAll.get()) return emptyList()
        return sweepSnapshots(workspace, repository)
    }

    private fun directoryNoun(count: Int): String {
        if (count == 1) return "directory"
        return "directories"
    }

    private fun deleteRecordedArtifacts(workspace: LocalWorkspace, repository: File): List<String> =
        workspace.libraries.flatMap { library ->
            val directories = library.modules.map { module -> moduleDirectory(repository, module, library.version) }
            deleteExisting(directories, repository)
        }

    private fun sweepSnapshots(workspace: LocalWorkspace, repository: File): List<String> {
        val groups = workspace.libraries
            .flatMap { library -> library.modules.map { module -> module.group } }
            .distinct()
        val directories = groups.flatMap { group -> snapshotDirectories(repository, group) }

        return deleteExisting(directories, repository)
    }

    private fun deleteExisting(directories: List<File>, repository: File): List<String> {
        val existing = directories.filter { directory -> directory.isDirectory }
        return existing.map { directory ->
            directory.deleteRecursively()
            pruneEmptyParents(directory.parentFile, repository)
            directory.absolutePath
        }
    }

    private fun pruneEmptyParents(start: File?, stop: File) {
        var current = start
        while (current != null && isPrunable(current, stop)) {
            if (!current.deleteRecursively()) return
            current = current.parentFile
        }
    }

    private fun isPrunable(directory: File, stop: File): Boolean {
        val isInsideRepository = directory != stop && directory.startsWith(stop)
        if (!isInsideRepository) return false

        val remaining = directory.listFiles().orEmpty()
        return remaining.all { it.isFile && it.name.startsWith(METADATA_PREFIX) }
    }

    private fun deleteRecords(directory: File, workspace: LocalWorkspace) {
        workspace.libraries.forEach { library ->
            val stateFile = stateFileOf(directory, library.group, library.library)
            stateFile.delete()
        }
        val isEmptyDirectory = directory.listFiles()?.isEmpty() == true
        if (isEmptyDirectory) directory.delete()
    }

    private companion object {
        private const val METADATA_PREFIX: String = "maven-metadata"
    }
}
