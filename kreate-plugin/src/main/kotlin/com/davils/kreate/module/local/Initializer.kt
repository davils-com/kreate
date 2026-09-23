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

package com.davils.kreate.module.local

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.local.tasks.LocalClean
import com.davils.kreate.module.local.tasks.LocalPublish
import com.davils.kreate.module.local.tasks.LocalPublishAll
import com.davils.kreate.module.local.tasks.LocalStatus
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * The name of the task Gradle's `maven-publish` plugin provides.
 *
 * @since 3.2.0
 */
private const val PUBLISH_TO_MAVEN_LOCAL_TASK: String = "publishToMavenLocal"

/**
 * The Gradle property that widens [LocalClean] into a full sweep.
 *
 * @since 3.2.0
 */
private const val CLEAN_ALL_PROPERTY: String = "kreate.local.clean.all"

/**
 * Registers the local development tasks.
 *
 * Only ever called on the root project. The three tasks describe a repository and a machine
 * rather than a module: a multiplatform library installs a dozen coordinates from half as many
 * projects, and one record per project would be both racy to write and impossible to remove as a
 * unit.
 *
 * @param extension The main Kreate extension.
 * @since 3.2.0
 */
internal fun Project.initializeLocalWorkflow(extension: KreateExtension) {
    val local = extension.local
    if (!local.enabled.get()) return

    val state = stateDirectoryOf(providers, gradle.gradleUserHomeDir)
    val repository = mavenLocalOf(providers)
    val inCi = local.ciEnvironmentVariables.get().any { variable ->
        providers.environmentVariable(variable).getOrElse("").isNotBlank()
    }

    registerLocalPublish(state, repository, inCi)
    registerLocalStatus(extension, state, repository)
    registerLocalClean(state, repository, inCi)
    registerLocalPublishAll(extension)
}

/**
 * Registers [KreateTasks.Local.PUBLISH_ALL], if a workspace has been declared.
 *
 * Only the one repository that declares the workspace gets this task. Every other repository sees
 * three tasks, not four, which is the honest listing: a library cannot orchestrate a workspace it
 * does not know the shape of.
 *
 * @param extension The main Kreate extension.
 * @since 3.2.0
 */
private fun Project.registerLocalPublishAll(extension: KreateExtension) {
    val workspace = extension.local.workspace
    if (workspace.libraries.isEmpty()) return

    val root = workspace.root.orNull?.asFile ?: layout.projectDirectory.asFile.parentFile
    val declared = workspace.libraries.map { library ->
        WorkspaceLibrary(
            name = library.name,
            path = java.io.File(root, library.path.get()).absolutePath,
            dependencies = library.dependencies.get(),
            tasks = library.tasks.get()
        )
    }

    // Ordered here rather than in the task action, so that a cycle or an undeclared edge is a
    // configuration failure a developer sees immediately, not one they see after the first three
    // repositories have already been republished.
    val ordered = topologicalOrder(declared)

    tasks.register<LocalPublishAll>(KreateTasks.Local.PUBLISH_ALL) {
        libraries.set(ordered)
    }
}

/**
 * Registers [KreateTasks.Local.PUBLISH].
 *
 * @param state The directory recording what is published locally.
 * @param repository The local Maven repository.
 * @param inCi Whether the build is running in CI.
 * @since 3.2.0
 */
private fun Project.registerLocalPublish(
    state: java.io.File,
    repository: java.io.File,
    inCi: Boolean
) {
    // Both providers reach across projects, and both are resolved when the task graph is built —
    // by which time every project has been evaluated. Reading either here, inside the root
    // project's `afterEvaluate`, would see a build in which no subproject exists yet.
    val publishTasks = provider {
        allprojects.mapNotNull { candidate -> candidate.tasks.findByName(PUBLISH_TO_MAVEN_LOCAL_TASK) }
    }
    val coordinates = provider {
        allprojects
            .flatMap { candidate -> candidate.mavenCoordinates() }
            .distinct()
            .sorted()
    }
    val resolvedVersion = provider { version.toString() }

    val projectName = name
    val projectDirectory = layout.projectDirectory

    tasks.register<LocalPublish>(KreateTasks.Local.PUBLISH) {
        dependsOn(publishTasks)

        library.set(projectName)
        publishedVersion.set(resolvedVersion)
        this.coordinates.set(coordinates)
        kreateVersion.set(KREATE_VERSION)
        continuousIntegration.set(inCi)
        this.stateDirectory.set(state)
        mavenLocal.set(repository)
        repositoryDirectory.set(projectDirectory)
    }
}

/**
 * Registers [KreateTasks.Local.STATUS].
 *
 * @param extension The main Kreate extension.
 * @param state The directory recording what is published locally.
 * @param repository The local Maven repository.
 * @since 3.2.0
 */
private fun Project.registerLocalStatus(
    extension: KreateExtension,
    state: java.io.File,
    repository: java.io.File
) {
    // Resolved lazily so that a status report can still be produced on a machine whose state
    // would make the activation rule fail the build — which is exactly when it is wanted.
    val reason = provider {
        val mode = runCatching {
            resolveLocalMode(
                gatherLocalModeInputs(
                    providers,
                    gradle.gradleUserHomeDir,
                    extension.local.ciEnvironmentVariables.get()
                )
            )
        }.getOrElse { failure -> LocalMode.Inactive(failure.message.orEmpty().lines().first()) }

        (mode as? LocalMode.Inactive)?.reason
    }

    tasks.register<LocalStatus>(KreateTasks.Local.STATUS) {
        this.stateDirectory.set(state)
        mavenLocal.set(repository)
        inactiveReason.set(reason)
    }
}

/**
 * Registers [KreateTasks.Local.CLEAN].
 *
 * @param state The directory recording what is published locally.
 * @param repository The local Maven repository.
 * @param inCi Whether the build is running in CI.
 * @since 3.2.0
 */
private fun Project.registerLocalClean(
    state: java.io.File,
    repository: java.io.File,
    inCi: Boolean
) {
    val sweep = providers.gradleProperty(CLEAN_ALL_PROPERTY)
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

    tasks.register<LocalClean>(KreateTasks.Local.CLEAN) {
        this.stateDirectory.set(state)
        mavenLocal.set(repository)
        continuousIntegration.set(inCi)
        sweepAll.set(sweep)
    }
}

/**
 * The `group:name` coordinates this project publishes.
 *
 * Read off the publications rather than derived from the project's name, because a multiplatform
 * project publishes one coordinate per target and a Gradle plugin publishes a marker whose name
 * has nothing to do with either.
 *
 * @return The coordinates, or an empty list for a project that publishes nothing.
 * @since 3.2.0
 */
private fun Project.mavenCoordinates(): List<String> =
    extensions.findByType(PublishingExtension::class.java)
        ?.publications
        ?.withType<MavenPublication>()
        ?.map { publication -> "${publication.groupId}:${publication.artifactId}" }
        .orEmpty()
