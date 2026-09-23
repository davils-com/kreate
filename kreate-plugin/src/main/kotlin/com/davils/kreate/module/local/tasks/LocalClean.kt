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
import com.davils.kreate.module.local.LocalWorkspace
import com.davils.kreate.module.local.moduleDirectory
import com.davils.kreate.module.local.readLocalWorkspace
import com.davils.kreate.module.local.snapshotDirectories
import com.davils.kreate.module.local.stateFileOf
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Removes local publications and the records that point at them.
 *
 * By default it deletes exactly what the records name: every coordinate of every recorded
 * library, at the recorded version. Nothing is guessed, so a release that happens to sit in the
 * same repository — put there by a Maven build, or by a `publishToMavenLocal` predating this
 * feature — is never touched.
 *
 * `-Pdavils.local.clean.all=true` additionally sweeps every snapshot under the recorded groups,
 * which is the escape hatch for a repository left inconsistent by a record that was deleted
 * before the artefacts it named.
 *
 * @since 3.2.0
 */
@DisableCachingByDefault(because = "Deletes machine state outside the project directory.")
public abstract class LocalClean : Task(
    "Removes everything published to the local Maven repository by ${KreateTasks.Local.PUBLISH}.",
    KreateTasks.Local.GROUP
) {
    /**
     * The directory that records what is published locally.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    /**
     * The local Maven repository to clean.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    /**
     * Whether to sweep every snapshot under the recorded groups as well.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val sweepAll: Property<Boolean>

    /**
     * Whether the build is running in CI.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val continuousIntegration: Property<Boolean>

    /**
     * Deletes the publications and the records.
     *
     * @since 3.2.0
     */
    @TaskAction
    public fun clean() {
        check(!continuousIntegration.get()) {
            "${KreateTasks.Local.CLEAN} deletes from the machine's Maven repository and must " +
                "not run in CI."
        }

        val state = stateDirectory.get().asFile
        val repository = mavenLocal.get().asFile
        val workspace = readLocalWorkspace(state)

        if (workspace.isEmpty) {
            logger.lifecycle("Nothing is published locally. Nothing to clean.")
            return
        }

        val removed = deleteRecordedArtifacts(workspace, repository) +
            if (sweepAll.get()) sweepSnapshots(workspace, repository) else emptyList()

        removed.distinct().sorted().forEach { path -> logger.lifecycle("  removed $path") }
        deleteRecords(state, workspace)

        logger.lifecycle("")
        logger.lifecycle(
            "Removed ${removed.distinct().size} director${if (removed.distinct().size == 1) "y" else "ies"} " +
                "and ${workspace.libraries.size} record(s). Builds resolve released versions again."
        )
    }

    /**
     * Deletes exactly the coordinates the records name.
     *
     * @param workspace The recorded libraries.
     * @param repository The local Maven repository.
     * @return The paths that were removed.
     * @since 3.2.0
     */
    private fun deleteRecordedArtifacts(workspace: LocalWorkspace, repository: File): List<String> =
        workspace.libraries.flatMap { library ->
            library.modules
                .map { module -> moduleDirectory(repository, module, library.version) }
                .filter { directory -> directory.isDirectory }
                .map { directory ->
                    directory.deleteRecursively()
                    pruneEmptyParents(directory.parentFile, repository)
                    directory.absolutePath
                }
        }

    /**
     * Deletes every snapshot under the groups the records mention.
     *
     * @param workspace The recorded libraries.
     * @param repository The local Maven repository.
     * @return The paths that were removed.
     * @since 3.2.0
     */
    private fun sweepSnapshots(workspace: LocalWorkspace, repository: File): List<String> {
        val groups = workspace.libraries
            .flatMap { library -> library.modules.map { module -> module.group } }
            .distinct()

        return groups.flatMap { group -> snapshotDirectories(repository, group) }
            .filter { directory -> directory.isDirectory }
            .map { directory ->
                directory.deleteRecursively()
                pruneEmptyParents(directory.parentFile, repository)
                directory.absolutePath
            }
    }

    /**
     * Removes directories left empty by a deletion, up to but never including the repository root.
     *
     * A Maven layout that keeps the skeleton of a removed artefact is not broken, but it does make
     * the next `kreateLocalStatus` harder to read and leaves `maven-metadata-local.xml` files
     * pointing at versions that are gone.
     *
     * @param start The directory to begin at.
     * @param stop The repository root, which is never removed.
     * @since 3.2.0
     */
    private fun pruneEmptyParents(start: File?, stop: File) {
        var current = start
        while (current != null && current != stop && current.startsWith(stop)) {
            val remaining = current.listFiles().orEmpty()
            val onlyMetadata = remaining.all { it.isFile && it.name.startsWith(METADATA_PREFIX) }
            if (remaining.isNotEmpty() && !onlyMetadata) return

            if (!current.deleteRecursively()) return
            current = current.parentFile
        }
    }

    /**
     * Deletes the record of every library that was cleaned.
     *
     * @param directory The state directory.
     * @param workspace The recorded libraries.
     * @since 3.2.0
     */
    private fun deleteRecords(directory: File, workspace: LocalWorkspace) {
        workspace.libraries.forEach { library ->
            stateFileOf(directory, library.group, library.library).delete()
        }
        directory.listFiles()?.takeIf { it.isEmpty() }?.let { directory.delete() }
    }

    private companion object {
        /**
         * The prefix of the metadata files Maven writes next to a version directory.
         * @since 3.2.0
         */
        private const val METADATA_PREFIX: String = "maven-metadata"
    }
}
