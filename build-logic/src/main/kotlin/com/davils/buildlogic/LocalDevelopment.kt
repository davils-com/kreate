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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.ProviderFactory
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Properties

/**
 * Everything in this file is a deliberate duplicate of `com.davils.kreate.module.local` in the
 * plugin itself.
 *
 * `build-logic` compiles the conventions that build `kreate-plugin`, so it cannot depend on the
 * artefact `kreate-plugin` produces without forming a cycle. Kreate therefore cannot apply
 * itself, and the one project that most needs to be published locally — the plugin — is the one
 * project the plugin cannot serve.
 *
 * The duplicated surface is kept as small as the feature allows: the state file's name, its keys
 * and the snapshot suffix. Those three things are the contract between a producer and a
 * consumer, and a change to any of them has to be made in both places. The plugin-side
 * definitions in `LocalWorkspaceState.kt` are the reference; this file follows them.
 */

/**
 * The version suffix that marks a locally published build.
 *
 * Maven and Gradle both give `-SNAPSHOT` versions their own resolution semantics, and the local
 * Maven repository is declared with `mavenContent { snapshotsOnly() }` on the consumer side. A
 * locally published artefact therefore cannot shadow a release even if the two carry the same
 * base version.
 *
 * @since 3.2.0
 */
public const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"

/**
 * The name of the task that installs a build into the local Maven repository.
 *
 * Duplicated from `com.davils.kreate.KreateTasks.Local.PUBLISH`.
 *
 * @since 3.2.0
 */
public const val LOCAL_PUBLISH_TASK: String = "kreateLocalPublish"

/**
 * The task group shared by the local development tasks.
 *
 * Duplicated from `com.davils.kreate.KreateTasks.Local.GROUP`.
 *
 * @since 3.2.0
 */
public const val LOCAL_TASK_GROUP: String = "kreate local"

/**
 * The Gradle property that requests a local publish without naming the task.
 *
 * @since 3.2.0
 */
public const val LOCAL_PUBLISH_PROPERTY: String = "kreate.local.publish"

/**
 * The directory under the Gradle user home that records what is currently published locally.
 *
 * @since 3.2.0
 */
private const val STATE_DIRECTORY: String = "kreate/local"

/**
 * Environment variables whose presence means the build is running in CI.
 *
 * @since 3.2.0
 */
private val CI_VARIABLES: List<String> =
    listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")

/**
 * Whether this invocation asked for a local publish.
 *
 * Two forms are accepted, because they serve different callers. The property is the explicit
 * one, and is what an orchestrating build passes to a subprocess. The task name is the
 * convenient one: typing `./gradlew kreateLocalPublish` should not also require a flag that
 * says what the task name already says.
 *
 * The task names are read from the root build of the composite rather than from this build.
 * Gradle derives an included build's [Gradle.getStartParameter] from its parent's and clears the
 * requested task names, so `:kreate-plugin:kreateLocalPublish` is only visible from the root.
 *
 * @param gradle The build invocation.
 * @param providers The provider factory of the surrounding project.
 * @return `true` when the version should carry the snapshot suffix.
 * @since 3.2.0
 */
public fun requestsLocalPublish(gradle: Gradle, providers: ProviderFactory): Boolean {
    val requestedByProperty = providers.gradleProperty(LOCAL_PUBLISH_PROPERTY)
        .map { it.equals("true", ignoreCase = true) }
        .getOrElse(false)
    if (requestedByProperty) return true

    var root: Gradle = gradle
    while (root.parent != null) root = requireNotNull(root.parent)

    return root.startParameter.taskNames.any { requested ->
        requested.substringAfterLast(':') == LOCAL_PUBLISH_TASK
    }
}

/**
 * Whether the build is running in CI.
 *
 * @param providers The provider factory of the surrounding project.
 * @return `true` when any of the known CI environment variables is set and non-blank.
 * @since 3.2.0
 */
public fun isContinuousIntegration(providers: ProviderFactory): Boolean =
    CI_VARIABLES.any { variable ->
        providers.environmentVariable(variable).getOrElse("").isNotBlank()
    }

/**
 * The coordinates a locally published Gradle plugin build installs.
 *
 * `java-gradle-plugin` generates one marker publication per declared plugin id, named
 * `<id>:<id>.gradle.plugin`. The markers are derived from the declared ids rather than read off
 * `publishing.publications`, because the container is filled during that plugin's own
 * `afterEvaluate` and reading it from a convention would race with it.
 *
 * Call this from inside a task configuration action, which Gradle realises after every project
 * has been evaluated, so that the declared ids are complete. The result is a plain list rather
 * than a provider, so that a task action capturing it captures data and not a script object.
 *
 * @param project The project that publishes the plugin.
 * @param primary The coordinate of the artefact backing every declared plugin id.
 * @return The primary coordinate followed by one marker coordinate per declared plugin id.
 * @since 3.2.0
 */
public fun publishedPluginCoordinates(project: Project, primary: String): List<String> {
    val markers = project.extensions
        .findByType(GradlePluginDevelopmentExtension::class.java)
        ?.plugins
        ?.map { declared -> "${declared.id}:${declared.id}.gradle.plugin" }
        .orEmpty()
        .sorted()

    return listOf(primary) + markers
}

/**
 * Records a locally published build so that consumers can resolve it.
 *
 * Written whole to a sibling temporary file and then moved into place, because two repositories
 * publishing at the same time must not be able to observe each other's half-written file.
 *
 * @param gradleUserHome The Gradle user home holding the state directory.
 * @param group The group of the published artefacts.
 * @param library The name the state file is keyed by, normally the root project's name.
 * @param repository The absolute path of the repository the build came from.
 * @param version The published version, which must carry the snapshot suffix.
 * @param kreateVersion The version of Kreate that produced the publication.
 * @param modules The `group:name` coordinates that were published.
 * @return The file that was written.
 * @throws GradleException If the version does not carry the snapshot suffix.
 * @since 3.2.0
 */
@Suppress("LongParameterList")
public fun writeLocalState(
    gradleUserHome: File,
    group: String,
    library: String,
    repository: File,
    version: String,
    kreateVersion: String,
    modules: List<String>
): File {
    if (!version.endsWith(SNAPSHOT_SUFFIX)) {
        throw GradleException(
            "Refusing to record '$group:$library:$version' as a local build: a local " +
                "publication has to carry the '$SNAPSHOT_SUFFIX' suffix so that it can never " +
                "shadow a release."
        )
    }

    val directory = File(gradleUserHome, STATE_DIRECTORY)
    Files.createDirectories(directory.toPath())

    val contents = Properties().apply {
        setProperty("library", library)
        setProperty("group", group)
        setProperty("repository", repository.absolutePath)
        setProperty("version", version)
        setProperty("publishedAt", Instant.now().toString())
        setProperty("kreate", kreateVersion)
        setProperty("modules", modules.sorted().joinToString(","))
    }

    val target = File(directory, "$group.$library.properties")
    val temporary = File(directory, "$group.$library.properties.tmp")

    // A Writer rather than an OutputStream: the stream form escapes everything outside Latin-1,
    // and the repository path is the one value here that can legitimately contain anything.
    temporary.writer(StandardCharsets.UTF_8).use { writer ->
        contents.store(writer, "Written by $LOCAL_PUBLISH_TASK. Delete with kreateLocalClean.")
    }
    Files.move(
        temporary.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
    )

    return target
}
