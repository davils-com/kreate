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
import com.davils.kreate.settings.local.LocalLibrary
import com.davils.kreate.settings.local.missingArtifacts
import com.davils.kreate.settings.local.publishedAtInstant
import com.davils.kreate.settings.local.readLocalWorkspace
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Duration
import java.time.Instant

@DisableCachingByDefault(because = "Reports machine state that changes outside this build.")
internal abstract class LocalStatus : KreateTask(
    "Reports which libraries are resolved from the local Maven repository, or why none are.",
    KreateTaskGroup.LOCAL
) {
    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    @get:Internal
    public abstract val inactiveReason: Property<String>

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
            logger.lifecycle("Publish a library with:  cd <library> && ./gradlew ${LocalWorkflowTaskNames.PUBLISH}")
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
        reportMode()
    }

    private fun reportMode() {
        val reason = inactiveReason.orNull
        if (reason != null) {
            logger.lifecycle("Local mode is OFF for this build, because $reason.")
            return
        }
        logger.lifecycle("Local mode is ON. These versions are substituted in place of the released ones.")
    }

    private fun reportLibrary(library: LocalLibrary, repository: File) {
        val missing = missingArtifacts(repository, library)
        val marker = markerFor(missing)

        logger.lifecycle(
            "  $marker ${library.library.padEnd(NAME_WIDTH)} ${library.version.padEnd(VERSION_WIDTH)} " +
                "${age(library.publishedAtInstant()).padEnd(AGE_WIDTH)} ${library.repository}"
        )
        logger.lifecycle("      ${library.modules.size} module(s), published by Kreate ${library.kreateVersion}")

        if (missing.isNotEmpty()) {
            logger.warn(
                "      ! ${missing.size} recorded coordinate(s) are not installed: " +
                    "${missing.joinToString()}. Re-publish with " +
                    "cd ${library.repository} && ./gradlew ${LocalWorkflowTaskNames.PUBLISH}"
            )
        }
    }

    private fun markerFor(missing: List<String>): String {
        if (missing.isEmpty()) return COMPLETE_MARKER
        return INCOMPLETE_MARKER
    }

    private fun age(instant: Instant?): String {
        if (instant == null) return "unknown"

        val elapsed = Duration.between(instant, Instant.now())
        val isUnderAMinute = elapsed.isNegative || elapsed.toMinutes() < 1
        if (isUnderAMinute) return "just now"
        if (elapsed.toHours() < 1) return "${elapsed.toMinutes()} min ago"
        if (elapsed.toDays() < 1) return "${elapsed.toHours()} h ago"
        return "${elapsed.toDays()} d ago"
    }

    private fun plural(count: Int): String {
        if (count == 1) return "y is"
        return "ies are"
    }

    private companion object {
        private const val COMPLETE_MARKER: String = " "

        private const val INCOMPLETE_MARKER: String = "!"

        private const val NAME_WIDTH: Int = 16

        private const val VERSION_WIDTH: Int = 20

        private const val AGE_WIDTH: Int = 12
    }
}
