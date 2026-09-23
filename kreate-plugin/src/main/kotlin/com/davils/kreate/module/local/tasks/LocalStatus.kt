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
import com.davils.kreate.module.local.LocalLibrary
import com.davils.kreate.module.local.missingArtifacts
import com.davils.kreate.module.local.publishedAtInstant
import com.davils.kreate.module.local.readLocalWorkspace
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Reports what is currently published to the local Maven repository.
 *
 * Exists to answer one question — "why is my fix not being picked up" — which is the question a
 * feature that changes dependency resolution behind the scenes will always generate. It therefore
 * reports the *reason* local mode is off just as carefully as it reports what is on, and verifies
 * that every recorded coordinate is really installed rather than trusting the record.
 *
 * @since 3.2.0
 */
@DisableCachingByDefault(because = "Reports machine state that changes outside this build.")
public abstract class LocalStatus : Task(
    "Reports which libraries are resolved from the local Maven repository, or why none are.",
    KreateTasks.Local.GROUP
) {
    /**
     * The directory that records what is published locally.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    /**
     * The local Maven repository the records are verified against.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    /**
     * Why local mode is off, or `null` when it is on.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val inactiveReason: Property<String>

    /**
     * Prints the report.
     *
     * @since 3.2.0
     */
    @TaskAction
    public fun report() {
        val state = stateDirectory.get().asFile
        val repository = mavenLocal.get().asFile
        val workspace = readLocalWorkspace(state)

        logger.lifecycle("Local Maven repository: $repository")
        logger.lifecycle("State directory:        $state")
        logger.lifecycle("")

        if (workspace.isEmpty) {
            logger.lifecycle("Nothing is published locally.")
            logger.lifecycle("")
            logger.lifecycle("Publish a library with:  cd <library> && ./gradlew ${KreateTasks.Local.PUBLISH}")
            return
        }

        logger.lifecycle("${workspace.libraries.size} librar${plural(workspace.libraries.size)} published locally:")
        workspace.libraries.forEach { library -> reportLibrary(library, repository) }

        val duplicates = workspace.duplicateCoordinates
        if (duplicates.isNotEmpty()) {
            logger.warn("")
            logger.warn(
                "These coordinates are claimed by more than one library, and the later record " +
                    "wins: ${duplicates.joinToString()}."
            )
        }

        logger.lifecycle("")
        inactiveReason.orNull?.let { reason ->
            logger.lifecycle("Local mode is OFF for this build, because $reason.")
        } ?: logger.lifecycle("Local mode is ON. These versions are substituted in place of the released ones.")
    }

    /**
     * Prints one library's entry.
     *
     * @param library The recorded library.
     * @param repository The local Maven repository to verify against.
     * @since 3.2.0
     */
    private fun reportLibrary(library: LocalLibrary, repository: File) {
        val missing = missingArtifacts(repository, library)
        val marker = if (missing.isEmpty()) " " else "!"

        logger.lifecycle(
            "  $marker ${library.library.padEnd(NAME_WIDTH)} ${library.version.padEnd(VERSION_WIDTH)} " +
                "${age(library.publishedAtInstant()).padEnd(AGE_WIDTH)} ${library.repository}"
        )
        logger.lifecycle("      ${library.modules.size} module(s), published by Kreate ${library.kreateVersion}")

        if (missing.isNotEmpty()) {
            logger.warn(
                "      ! ${missing.size} recorded coordinate(s) are not installed: " +
                    "${missing.joinToString()}. Re-publish with " +
                    "cd ${library.repository} && ./gradlew ${KreateTasks.Local.PUBLISH}"
            )
        }
    }

    /**
     * Renders how long ago a publication happened.
     *
     * @param instant When it happened, or `null` if the record has no usable timestamp.
     * @return A short human readable age.
     * @since 3.2.0
     */
    private fun age(instant: Instant?): String {
        if (instant == null) return "unknown"

        val elapsed = Duration.between(instant, Instant.now())
        return when {
            elapsed.isNegative -> "just now"
            elapsed.toMinutes() < 1 -> "just now"
            elapsed.toHours() < 1 -> "${elapsed.toMinutes()} min ago"
            elapsed.toDays() < 1 -> "${elapsed.toHours()} h ago"
            else -> "${elapsed.toDays()} d ago"
        }
    }

    /**
     * Returns the plural suffix for a count.
     *
     * @param count The number of libraries.
     * @return `y` or `ies`, completing the word "librar".
     * @since 3.2.0
     */
    private fun plural(count: Int): String = if (count == 1) "y is" else "ies are"

    private companion object {
        /**
         * The column width of a library name.
         * @since 3.2.0
         */
        private const val NAME_WIDTH: Int = 16

        /**
         * The column width of a version.
         * @since 3.2.0
         */
        private const val VERSION_WIDTH: Int = 20

        /**
         * The column width of an age.
         * @since 3.2.0
         */
        private const val AGE_WIDTH: Int = 12
    }
}
