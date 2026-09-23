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
import com.davils.kreate.module.local.LocalModule
import com.davils.kreate.module.local.SNAPSHOT_SUFFIX
import com.davils.kreate.module.local.missingArtifacts
import com.davils.kreate.module.local.writeLocalLibrary
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.Instant

/**
 * Records that this build has been installed into the local Maven repository.
 *
 * The publishing itself is `publishToMavenLocal`, which this task depends on and does not
 * replace. All this does is write the record that makes a consumer substitute the result in —
 * and refuse to do so when the conditions that make the feature safe do not hold.
 *
 * Registered on the root project only. A local publish is an act on a whole repository: a
 * multiplatform library installs a dozen coordinates from half as many projects, and a record per
 * project would be both racy to write and impossible to clean up as a unit.
 *
 * @since 3.2.0
 */
@DisableCachingByDefault(because = "Writes a timestamped record outside the project directory.")
public abstract class LocalPublish : Task(
    "Installs this repository into the local Maven repository so other checkouts can resolve it.",
    KreateTasks.Local.GROUP
) {
    /**
     * The name the record is keyed by, normally the root project's name.
     * @since 3.2.0
     */
    @get:Input
    public abstract val library: Property<String>

    /**
     * The version being published, which has to carry the snapshot suffix.
     * @since 3.2.0
     */
    @get:Input
    public abstract val publishedVersion: Property<String>

    /**
     * The `group:name` coordinates every project in this build publishes.
     * @since 3.2.0
     */
    @get:Input
    public abstract val coordinates: ListProperty<String>

    /**
     * The version of Kreate that produced the publication.
     * @since 3.2.0
     */
    @get:Input
    public abstract val kreateVersion: Property<String>

    /**
     * Whether the build is running in CI.
     * @since 3.2.0
     */
    @get:Input
    public abstract val continuousIntegration: Property<Boolean>

    /**
     * The directory that records what is published locally.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val stateDirectory: DirectoryProperty

    /**
     * The local Maven repository the publication was installed into.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val mavenLocal: DirectoryProperty

    /**
     * The repository this build was run from.
     * @since 3.2.0
     */
    @get:Internal
    public abstract val repositoryDirectory: DirectoryProperty

    /**
     * Verifies the publication and records it.
     *
     * @since 3.2.0
     */
    @TaskAction
    public fun record() {
        val version = publishedVersion.get()

        check(!continuousIntegration.get()) {
            "${KreateTasks.Local.PUBLISH} is a developer workflow and must not run in CI. A " +
                "pipeline publishes to a shared registry from a tag, which is a different thing " +
                "entirely — and an artefact built from a local publication could not be " +
                "reproduced by anyone else."
        }
        check(version.endsWith(SNAPSHOT_SUFFIX)) {
            "Refusing to record '$version' as a local build: a local publication has to carry " +
                "the '$SNAPSHOT_SUFFIX' suffix so that it can never shadow a release. Run the " +
                "task by name, or pass -Pdavils.local.publish=true."
        }

        val modules = coordinates.get().mapNotNull(LocalModule.Companion::parse)
        check(modules.isNotEmpty()) {
            "${KreateTasks.Local.PUBLISH} found no Maven publications in this build. Enable " +
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

        // `publishToMavenLocal` has already run by now, so a gap here means the artefacts were
        // never written where this build expects to find them — most often a `maven.repo.local`
        // that points somewhere other than the consumer will look.
        val missing = missingArtifacts(mavenLocal.get().asFile, record)
        check(missing.isEmpty()) {
            "publishToMavenLocal reported success but these coordinates are not in " +
                "${mavenLocal.get().asFile}: ${missing.joinToString()}."
        }

        val state = writeLocalLibrary(stateDirectory.get().asFile, record)

        logger.lifecycle("Published ${record.library}:${record.version} to ${mavenLocal.get().asFile}.")
        modules.forEach { module -> logger.lifecycle("    ${module.coordinate}") }
        logger.lifecycle("")
        logger.lifecycle("Other checkouts will now resolve these instead of the released versions.")
        logger.lifecycle("Undo with ./gradlew ${KreateTasks.Local.CLEAN}. Recorded in $state.")
    }

    /**
     * The group recorded for this publication.
     *
     * A build can publish under more than one group — a Gradle plugin installs its marker under
     * the plugin id, which is a group of its own — so the shortest is taken as the primary. It is
     * only ever used for the record's file name and for display; substitution and cleaning both
     * work from the full coordinates.
     *
     * @param modules The published coordinates.
     * @return The shortest group among them.
     * @since 3.2.0
     */
    private fun primaryGroupOf(modules: List<LocalModule>): String =
        modules.map { it.group }.minByOrNull { it.length }.orEmpty()
}
