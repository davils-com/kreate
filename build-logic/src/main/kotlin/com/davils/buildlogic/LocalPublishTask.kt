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

package com.davils.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Records that this build has been installed into the local Maven repository.
 *
 * A real task class rather than a `doLast` block in the convention plugin, and that is not a
 * stylistic choice. Kotlin compiles a lambda inside a precompiled script plugin into a class
 * holding a `this$0` reference to the script, and the configuration cache refuses to serialize a
 * script object. Any task action written inline in `kreate.publish-conventions.gradle.kts` fails
 * the build, because `org.gradle.configuration-cache.problems=fail`.
 *
 * The publishing itself is not done here — `publishToMavenLocal` does that, and this task depends
 * on it. All this does is write the file that tells a consumer the publication exists.
 *
 * @since 3.2.0
 */
@DisableCachingByDefault(because = "Writes a timestamped record outside the project directory.")
public abstract class LocalPublishTask : DefaultTask() {
    /**
     * The `group:name` coordinates the publication installed.
     *
     * @since 3.2.0
     */
    @get:Input
    public abstract val coordinates: ListProperty<String>

    /**
     * The group of the published artefacts.
     *
     * @since 3.2.0
     */
    @get:Input
    public abstract val publishedGroup: Property<String>

    /**
     * The name the state file is keyed by.
     *
     * @since 3.2.0
     */
    @get:Input
    public abstract val library: Property<String>

    /**
     * The published version, which has to carry the snapshot suffix.
     *
     * @since 3.2.0
     */
    @get:Input
    public abstract val publishedVersion: Property<String>

    /**
     * Whether the build is running in CI.
     *
     * @since 3.2.0
     */
    @get:Input
    public abstract val runningInCi: Property<Boolean>

    /**
     * The Gradle user home holding the state directory.
     *
     * Not an input: the state file lives outside the project and its location says nothing about
     * whether the publication is current.
     *
     * @since 3.2.0
     */
    @get:Internal
    public abstract val gradleUserHome: DirectoryProperty

    /**
     * The repository the publication was built from, recorded so that `kreateLocalStatus` can
     * point a developer at the working copy that produced it.
     *
     * @since 3.2.0
     */
    @get:Internal
    public abstract val repositoryDirectory: DirectoryProperty

    /**
     * Writes the state file.
     *
     * @since 3.2.0
     */
    @TaskAction
    public fun record() {
        val version = publishedVersion.get()

        check(!runningInCi.get()) {
            "$LOCAL_PUBLISH_TASK is a developer workflow and must not run in CI. A pipeline " +
                "publishes to a shared registry from a tag, which is a different thing entirely."
        }
        check(version.endsWith(SNAPSHOT_SUFFIX)) {
            "Refusing to install '$version' into the local Maven repository: a local publication " +
                "has to carry the '$SNAPSHOT_SUFFIX' suffix so that it can never shadow a " +
                "release. Run the task by name, or pass -P$LOCAL_PUBLISH_PROPERTY=true."
        }

        val published = coordinates.get()
        val state = writeLocalState(
            gradleUserHome = gradleUserHome.get().asFile,
            group = publishedGroup.get(),
            library = library.get(),
            repository = repositoryDirectory.get().asFile,
            version = version,
            kreateVersion = version,
            modules = published
        )

        logger.lifecycle(
            "Published ${publishedGroup.get()}:${library.get()}:$version to the local Maven repository."
        )
        published.forEach { coordinate -> logger.lifecycle("    $coordinate") }
        logger.lifecycle("Recorded in ${state.absolutePath}.")
    }
}
