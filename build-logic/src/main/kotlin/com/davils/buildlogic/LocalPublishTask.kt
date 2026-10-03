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

@DisableCachingByDefault(because = "Writes a timestamped record outside the project directory.")
public abstract class LocalPublishTask : DefaultTask() {
    @get:Input
    public abstract val coordinates: ListProperty<String>

    @get:Input
    public abstract val publishedGroup: Property<String>

    @get:Input
    public abstract val library: Property<String>

    @get:Input
    public abstract val publishedVersion: Property<String>

    @get:Input
    public abstract val runningInCi: Property<Boolean>

    @get:Internal
    public abstract val gradleUserHome: DirectoryProperty

    @get:Internal
    public abstract val repositoryDirectory: DirectoryProperty

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
        val userHome = gradleUserHome.get()
        val repository = repositoryDirectory.get()
        val state = writeLocalState(
            gradleUserHome = userHome.asFile,
            group = publishedGroup.get(),
            library = library.get(),
            repository = repository.asFile,
            version = version,
            kreateVersion = version,
            modules = published
        )

        val coordinate = "${publishedGroup.get()}:${library.get()}:$version"
        logger.lifecycle("Published $coordinate to the local Maven repository.")
        published.forEach { module -> logger.lifecycle("    $module") }
        logger.lifecycle("Recorded in ${state.absolutePath}.")
    }
}
