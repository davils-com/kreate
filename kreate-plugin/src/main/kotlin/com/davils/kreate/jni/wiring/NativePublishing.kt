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

package com.davils.kreate.jni.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.host.requireKnownPlatform
import com.davils.kreate.jni.JniPublishingExtension
import com.davils.kreate.jni.JniTaskNames
import com.davils.kreate.jni.task.PLATFORM_CANDIDATE_SEPARATOR
import com.davils.kreate.jni.task.VerifyNativePlatforms
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register
import java.io.File

internal const val PLATFORM_SELECTION_PROPERTY: String = "kreate.jni.publishPlatforms"

private const val PLATFORM_LIST_SEPARATOR: Char = ','

private const val DECLARED_PLATFORMS_ORIGIN: String = "jni { packaging { publishing { platforms } } }"

private const val NATIVE_JAR_DIRECTORY: String = "libs"

internal fun Project.configureNativePublishing(
    extension: KreateExtension,
    projectName: String,
    hostLibraryDir: Provider<out Directory>,
    hostPlatformId: String,
    resourcePath: String,
    buildTaskName: String
) {
    val publishing = extension.platform.jvm.jni.packaging.publishing
    val platforms = resolveSelectedPlatforms(publishing, hostPlatformId)
    val sources = platforms.map { platformId ->
        NativeSource(
            platformId = platformId,
            staged = publishing.stagingDirectory.takeIf { it.isPresent }?.map { it.dir(platformId) },
            hostBuilt = hostLibraryDir.takeIf { platformId == hostPlatformId }
        )
    }

    val verify = registerVerifyTask(sources, buildTaskName, hostPlatformId)
    val jars = sources.map { source ->
        registerNativeJar(source, projectName, resourcePath, verify, buildTaskName, hostPlatformId)
    }

    tasks.register(JniTaskNames.NATIVE_JARS) {
        group = KreateTaskGroup.JNI.label
        description = "Builds the native JAR of every platform selected for publishing."
        dependsOn(jars)
    }

    registerNativePublications(extension, projectName, sources.map { it.platformId }, jars)
}

internal fun Project.resolveSelectedPlatforms(
    publishing: JniPublishingExtension,
    hostPlatformId: String
): List<String> {
    val fromProperty = platformsFromProperty()
    if (fromProperty != null) return requireKnownPlatforms(fromProperty, "-P$PLATFORM_SELECTION_PROPERTY")

    val declared = publishing.platforms.orNull?.takeIf { it.isNotEmpty() } ?: listOf(hostPlatformId)
    return requireKnownPlatforms(declared, DECLARED_PLATFORMS_ORIGIN)
}

private fun Project.platformsFromProperty(): List<String>? {
    val propertyValue = providers.gradleProperty(PLATFORM_SELECTION_PROPERTY).orNull ?: return null
    val entries = propertyValue.split(PLATFORM_LIST_SEPARATOR)
    return entries.map { it.trim() }.filter { it.isNotEmpty() }
}

private fun requireKnownPlatforms(platforms: List<String>, origin: String): List<String> =
    platforms.distinct().map { requireKnownPlatform(it, origin) }

private fun Project.registerVerifyTask(
    sources: List<NativeSource>,
    buildTaskName: String,
    hostPlatformId: String
): TaskProvider<VerifyNativePlatforms> =
    tasks.register<VerifyNativePlatforms>(JniTaskNames.VERIFY_PLATFORMS) {
        if (sources.any { it.platformId == hostPlatformId }) {
            dependsOn(buildTaskName)
        }

        platforms.set(sources.map { it.platformId })
        searchedDirectories.from(sources.flatMap { it.directoriesByPrecedence() })
        candidatePaths.set(sources.map { source -> source.candidatePathEntry() })
    }

private fun NativeSource.candidatePathEntry(): String {
    val paths = directoriesByPrecedence().map { provider -> provider.get().asFile.absolutePath }
    return "$platformId$PLATFORM_CANDIDATE_SEPARATOR${paths.joinToString(File.pathSeparator)}"
}

private fun Project.registerNativeJar(
    source: NativeSource,
    projectName: String,
    resourcePath: String,
    verify: TaskProvider<VerifyNativePlatforms>,
    buildTaskName: String,
    hostPlatformId: String
): TaskProvider<Jar> =
    tasks.register<Jar>(JniTaskNames.nativeJar(source.platformId)) {
        group = KreateTaskGroup.JNI.label
        description = "Packages the ${source.platformId} native library of $projectName."

        dependsOn(verify)
        if (source.platformId == hostPlatformId) {
            dependsOn(buildTaskName)
        }

        source.directoriesByPrecedence().forEach { directory ->
            from(directory) { into("$resourcePath/${source.platformId}") }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveBaseName.set(projectName)
        archiveAppendix.set(source.platformId)
        destinationDirectory.set(layout.buildDirectory.dir(NATIVE_JAR_DIRECTORY))
    }
