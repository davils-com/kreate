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
import com.davils.kreate.settings.local.LocalModule
import com.davils.kreate.settings.local.SNAPSHOT_SUFFIX
import com.davils.kreate.settings.local.missingArtifacts
import com.davils.kreate.settings.local.writeLocalLibrary
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Instant

@DisableCachingByDefault(because = "Writes a timestamped record outside the project directory.")
internal abstract class LocalPublish : KreateTask(
    "Installs this repository into the local Maven repository so other checkouts can resolve it.",
    KreateTaskGroup.LOCAL
) {
    @get:Input
    public abstract val library: Property<String>

    @get:Input
    public abstract val publishedVersion: Property<String>

    @get:Input
    public abstract val coordinates: ListProperty<String>

    @get:Input
    public abstract val kreateVersion: Property<String>

    @get:Input
    public abstract val continuousIntegration: Property<Boolean>

    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    @get:Internal
    public abstract val repositoryDirectory: DirectoryProperty

    @TaskAction
    public fun record() {
        val version = publishedVersion.get()

        check(!continuousIntegration.get()) {
            "${LocalWorkflowTaskNames.PUBLISH} is a developer workflow and must not run in CI. A " +
                "pipeline publishes to a shared registry from a tag, which is a different thing " +
                "entirely — and an artefact built from a local publication could not be " +
                "reproduced by anyone else."
        }
        check(version.endsWith(SNAPSHOT_SUFFIX)) {
            "Refusing to record '$version' as a local build: a local publication has to carry " +
                "the '$SNAPSHOT_SUFFIX' suffix so that it can never shadow a release. Run the " +
                "task by name, or pass -Pkreate.local.publish=true."
        }

        val modules = coordinates.get().mapNotNull(LocalModule.Companion::parse)
        check(modules.isNotEmpty()) {
            "${LocalWorkflowTaskNames.PUBLISH} found no Maven publications in this build. Enable " +
                "publishing with `kreate { project { publish { enabled = true } } }` in the " +
                "projects that should be installed."
        }

        val record = LocalLibrary(
            library = library.get(),
            group = primaryGroupOf(modules),
            repository = repositoryDirectory.get().asFile,
            version = version,
            publishedAt = Instant.now().toString(),
            kreateVersion = kreateVersion.get(),
            modules = modules
        )

        verifyWrittenToMavenLocal(record)

        val state = writeLocalLibrary(stateDirectory.get().asFile, record)

        logger.lifecycle("Published ${record.library}:${record.version} to ${mavenLocal.get().asFile}.")
        modules.forEach { module -> logger.lifecycle("    ${module.coordinate}") }
        logger.lifecycle("")
        logger.lifecycle("Other checkouts will now resolve these instead of the released versions.")
        logger.lifecycle("Undo with ./gradlew ${LocalWorkflowTaskNames.CLEAN}. Recorded in $state.")
    }

    private fun verifyWrittenToMavenLocal(record: LocalLibrary) {
        val repository = mavenLocal.get().asFile
        val missing = missingArtifacts(repository, record)
        check(missing.isEmpty()) {
            "publishToMavenLocal reported success but these coordinates are not in " +
                "$repository: ${missing.joinToString()}."
        }
    }

    private fun primaryGroupOf(modules: List<LocalModule>): String {
        val groups = modules.map { it.group }
        return groups.minByOrNull { it.length }.orEmpty()
    }
}
