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

public const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"

public const val LOCAL_PUBLISH_TASK: String = "kreateLocalPublish"

public const val LOCAL_TASK_GROUP: String = "kreate local"

public const val LOCAL_PUBLISH_PROPERTY: String = "kreate.local.publish"

private const val STATE_DIRECTORY: String = "kreate/local"

private const val TASK_PATH_SEPARATOR: Char = ':'

private val CI_VARIABLES: List<String> = listOf("CI", "GITLAB_CI", "GITHUB_ACTIONS", "CI_PIPELINE_ID")

public fun requestsLocalPublish(gradle: Gradle, providers: ProviderFactory): Boolean {
    val property = providers.gradleProperty(LOCAL_PUBLISH_PROPERTY)
    val isRequestedByProperty = property.map { value -> value.equals("true", ignoreCase = true) }
    if (isRequestedByProperty.getOrElse(false)) return true

    val requestedTasks = rootOf(gradle).startParameter.taskNames
    return requestedTasks.any { requested -> requested.substringAfterLast(TASK_PATH_SEPARATOR) == LOCAL_PUBLISH_TASK }
}

public fun isContinuousIntegration(providers: ProviderFactory): Boolean =
    CI_VARIABLES.any { variable -> isSet(providers, variable) }

public fun publishedPluginCoordinates(project: Project, primary: String): List<String> {
    val development = project.extensions.findByType(GradlePluginDevelopmentExtension::class.java)
    val declaredPlugins = development?.plugins.orEmpty()
    val markers = declaredPlugins.map { declared -> "${declared.id}:${declared.id}.gradle.plugin" }
    return listOf(primary) + markers.sorted()
}

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

    val sortedModules = modules.sorted()
    val contents = Properties()
    contents.setProperty("library", library)
    contents.setProperty("group", group)
    contents.setProperty("repository", repository.absolutePath)
    contents.setProperty("version", version)
    contents.setProperty("publishedAt", Instant.now().toString())
    contents.setProperty("kreate", kreateVersion)
    contents.setProperty("modules", sortedModules.joinToString(","))

    val target = File(directory, "$group.$library.properties")
    val temporary = File(directory, "$group.$library.properties.tmp")
    writeUtf8(temporary, contents)
    Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    return target
}

private fun writeUtf8(file: File, contents: Properties) {
    file.writer(StandardCharsets.UTF_8).use { writer ->
        contents.store(writer, "Written by $LOCAL_PUBLISH_TASK. Delete with kreateLocalClean.")
    }
}

private fun rootOf(gradle: Gradle): Gradle {
    val parent = gradle.parent ?: return gradle
    return rootOf(parent)
}

private fun isSet(providers: ProviderFactory, variable: String): Boolean {
    val value = providers.environmentVariable(variable)
    return value.getOrElse("").isNotBlank()
}
