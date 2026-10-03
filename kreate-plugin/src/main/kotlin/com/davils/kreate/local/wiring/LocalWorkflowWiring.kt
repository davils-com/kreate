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

package com.davils.kreate.local.wiring

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.KreateExtension
import com.davils.kreate.local.LocalWorkflowTaskNames
import com.davils.kreate.local.LocalWorkspaceExtension
import com.davils.kreate.local.task.LocalClean
import com.davils.kreate.local.task.LocalPublish
import com.davils.kreate.local.task.LocalPublishAll
import com.davils.kreate.local.task.LocalStatus
import com.davils.kreate.local.workspace.WorkspaceLibrary
import com.davils.kreate.local.workspace.topologicalOrder
import com.davils.kreate.settings.local.LocalMode
import com.davils.kreate.settings.local.LocalModeInputs
import com.davils.kreate.settings.local.gatherLocalModeInputs
import com.davils.kreate.settings.local.mavenLocalOf
import com.davils.kreate.settings.local.resolveLocalMode
import com.davils.kreate.settings.local.stateDirectoryOf
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

private const val PUBLISH_TO_MAVEN_LOCAL_TASK: String = "publishToMavenLocal"

private const val CLEAN_ALL_PROPERTY: String = "kreate.local.clean.all"

private const val ENABLED_VALUE: String = "true"

internal fun Project.initializeLocalWorkflow(extension: KreateExtension) {
    val local = extension.local
    if (!local.enabled.get()) return

    val state = stateDirectoryOf(providers, gradle.gradleUserHomeDir)
    val repository = mavenLocalOf(providers)
    val ciVariables = local.ciEnvironmentVariables.get()
    val inCi = ciVariables.any { variable -> isEnvironmentVariableSet(variable) }

    registerLocalPublish(state, repository, inCi)
    registerLocalStatus(extension, state, repository)
    registerLocalClean(state, repository, inCi)
    registerLocalPublishAll(extension)
}

private fun Project.isEnvironmentVariableSet(variable: String): Boolean {
    val value = providers.environmentVariable(variable).getOrElse("")
    return value.isNotBlank()
}

private fun Project.registerLocalPublishAll(extension: KreateExtension) {
    val workspace = extension.local.workspace
    if (workspace.libraries.isEmpty()) return

    val declared = declaredWorkspaceLibraries(workspace)
    val validatedAtConfiguration = topologicalOrder(declared)

    tasks.register<LocalPublishAll>(LocalWorkflowTaskNames.PUBLISH_ALL) {
        libraries.set(validatedAtConfiguration)
    }
}

private fun Project.declaredWorkspaceLibraries(workspace: LocalWorkspaceExtension): List<WorkspaceLibrary> {
    val root = workspace.root.orNull?.asFile ?: layout.projectDirectory.asFile.parentFile

    return workspace.libraries.map { library ->
        WorkspaceLibrary(
            name = library.name,
            path = File(root, library.path.get()).absolutePath,
            dependencies = library.dependencies.get(),
            tasks = library.tasks.get()
        )
    }
}

private fun Project.registerLocalPublish(
    state: File,
    repository: File,
    inCi: Boolean
) {
    val publishTasksOnceAllProjectsAreEvaluated = provider {
        allprojects.mapNotNull { candidate -> candidate.tasks.findByName(PUBLISH_TO_MAVEN_LOCAL_TASK) }
    }
    val coordinatesOnceAllProjectsAreEvaluated = provider { coordinatesOfAllProjects() }
    val resolvedVersion = provider { version.toString() }

    val projectName = name
    val projectDirectory = layout.projectDirectory

    tasks.register<LocalPublish>(LocalWorkflowTaskNames.PUBLISH) {
        dependsOn(publishTasksOnceAllProjectsAreEvaluated)

        library.set(projectName)
        publishedVersion.set(resolvedVersion)
        coordinates.set(coordinatesOnceAllProjectsAreEvaluated)
        kreateVersion.set(KREATE_VERSION)
        continuousIntegration.set(inCi)
        stateDirectory.set(state)
        mavenLocal.set(repository)
        repositoryDirectory.set(projectDirectory)
    }
}

private fun Project.coordinatesOfAllProjects(): List<String> {
    val coordinates = allprojects.flatMap { candidate -> candidate.mavenCoordinates() }
    return coordinates.distinct().sorted()
}

private fun Project.registerLocalStatus(
    extension: KreateExtension,
    state: File,
    repository: File
) {
    val reasonResolvedEvenWhenActivationFails = provider { inactiveReasonOf(extension) }

    tasks.register<LocalStatus>(LocalWorkflowTaskNames.STATUS) {
        stateDirectory.set(state)
        mavenLocal.set(repository)
        inactiveReason.set(reasonResolvedEvenWhenActivationFails)
    }
}

private fun Project.inactiveReasonOf(extension: KreateExtension): String? {
    val mode = try {
        resolveLocalMode(localModeInputsOf(extension))
    } catch (failure: GradleException) {
        logger.info("Local mode cannot be activated for this build.", failure)
        return firstParagraphOf(failure.message.orEmpty())
    }

    return (mode as? LocalMode.Inactive)?.reason
}

private fun Project.localModeInputsOf(extension: KreateExtension): LocalModeInputs {
    val ciVariables = extension.local.ciEnvironmentVariables.get()
    return gatherLocalModeInputs(providers, gradle.gradleUserHomeDir, ciVariables)
}

private fun Project.registerLocalClean(
    state: File,
    repository: File,
    inCi: Boolean
) {
    val requestedSweep = providers.gradleProperty(CLEAN_ALL_PROPERTY)
    val sweep = requestedSweep.map { value -> value.equals(ENABLED_VALUE, ignoreCase = true) }.orElse(false)

    tasks.register<LocalClean>(LocalWorkflowTaskNames.CLEAN) {
        stateDirectory.set(state)
        mavenLocal.set(repository)
        continuousIntegration.set(inCi)
        sweepAll.set(sweep)
    }
}

private fun Project.mavenCoordinates(): List<String> {
    val publishing = extensions.findByType(PublishingExtension::class.java) ?: return emptyList()
    val publications = publishing.publications.withType<MavenPublication>()
    return publications.map { publication -> "${publication.groupId}:${publication.artifactId}" }
}
