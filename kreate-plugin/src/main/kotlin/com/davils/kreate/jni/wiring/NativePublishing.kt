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
import com.davils.kreate.jni.stage.STAGE_MANIFEST_FILE_NAME
import com.davils.kreate.jni.task.DIGEST_MANIFEST_FILE_NAME
import com.davils.kreate.jni.task.GenerateDigestManifest
import com.davils.kreate.jni.task.PLATFORM_CANDIDATE_SEPARATOR
import com.davils.kreate.jni.task.StageNativeLibraries
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

internal const val STAGE_PROPERTY: String = "kreate.jni.stage"

private const val STAGE_DIRECTORY: String = "build/kreate/jni-stage"

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
    val artifactBase = extension.project.name.orNull ?: projectName
    val host = HostLibrary(hostPlatformId, hostLibraryDir, buildTaskName)
    val stageRoot = stageRootOf(artifactBase)
    val sources = platforms.map { platformId -> nativeSourceOf(platformId, publishing, stageRoot, host) }

    registerStageTask(artifactBase, host)
    val verify = registerVerifyTask(sources, artifactBase, host)
    val writesDigests = extension.platform.jvm.jni.packaging.digestManifest.get()
    val jars = sources.map { source ->
        val digests = registerPlatformDigestManifest(source, verify, writesDigests)
        registerNativeJar(source, digests, projectName, resourcePath, verify, buildTaskName)
    }

    tasks.register(JniTaskNames.NATIVE_JARS) {
        group = KreateTaskGroup.JNI.label
        description = "Builds the native JAR of every platform selected for publishing."
        dependsOn(jars)
    }

    registerNativePublications(extension, projectName, sources.map { it.platformId }, jars, verify)
}

private fun Project.stageRootOf(artifactBase: String): Directory? {
    val stagePath = providers.gradleProperty(STAGE_PROPERTY).orNull?.takeIf { it.isNotBlank() } ?: return null
    val stage = layout.settingsDirectory.dir(stagePath)
    return stage.dir(artifactBase)
}

private fun Project.nativeSourceOf(
    platformId: String,
    publishing: JniPublishingExtension,
    stageRoot: Directory?,
    host: HostLibrary
): NativeSource {
    if (stageRoot != null) {
        val staged = stageRoot.dir(platformId)
        return NativeSource(platformId = platformId, staged = provider { staged }, hostBuilt = null)
    }

    return NativeSource(
        platformId = platformId,
        staged = publishing.stagingDirectory.takeIf { it.isPresent }?.map { it.dir(platformId) },
        hostBuilt = host.directory.takeIf { platformId == host.platformId }
    )
}

private fun Project.registerStageTask(artifactBase: String, host: HostLibrary) {
    val stageDirectory = layout.settingsDirectory.dir("$STAGE_DIRECTORY/$artifactBase/${host.platformId}")
    tasks.register<StageNativeLibraries>(JniTaskNames.STAGE) {
        dependsOn(host.buildTaskName)
        libraryDirectory.set(host.directory)
        platformId.set(host.platformId)
        artifactGroup.set(project.group.toString())
        artifactName.set(artifactBase)
        artifactVersion.set(project.version.toString())
        projectDirectory.set(layout.projectDirectory)
        this.stageDirectory.set(stageDirectory)
    }
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
    artifactBase: String,
    host: HostLibrary
): TaskProvider<VerifyNativePlatforms> =
    tasks.register<VerifyNativePlatforms>(JniTaskNames.VERIFY_PLATFORMS) {
        if (sources.any { source -> source.hostBuilt != null }) {
            dependsOn(host.buildTaskName)
        }

        platforms.set(sources.map { it.platformId })
        searchedDirectories.from(sources.flatMap { it.directoriesByPrecedence() })
        candidatePaths.set(sources.map { source -> source.candidatePathEntry() })
        stagedPaths.set(sources.mapNotNull { source -> source.stagedPathEntry() })
        artifactGroup.set(project.group.toString())
        artifactName.set(artifactBase)
        artifactVersion.set(project.version.toString())
        projectDirectory.set(layout.projectDirectory)
    }

private fun NativeSource.stagedPathEntry(): String? {
    val directory = staged ?: return null
    val path = directory.get().asFile.absolutePath
    return "$platformId$PLATFORM_CANDIDATE_SEPARATOR$path"
}

private fun NativeSource.candidatePathEntry(): String {
    val paths = directoriesByPrecedence().map { provider -> provider.get().asFile.absolutePath }
    return "$platformId$PLATFORM_CANDIDATE_SEPARATOR${paths.joinToString(File.pathSeparator)}"
}

private fun Project.registerPlatformDigestManifest(
    source: NativeSource,
    verify: TaskProvider<VerifyNativePlatforms>,
    writesDigests: Boolean
): TaskProvider<GenerateDigestManifest>? {
    if (!writesDigests) return null

    val platform = source.platformId
    return tasks.register<GenerateDigestManifest>(JniTaskNames.nativeDigestManifest(platform)) {
        dependsOn(verify)
        libraryDirectories.from(source.directoriesByPrecedence())
        platformId.set(platform)
        manifest.set(layout.buildDirectory.file("jni/$platform/published/$DIGEST_MANIFEST_FILE_NAME"))
    }
}

private fun Project.registerNativeJar(
    source: NativeSource,
    digests: TaskProvider<GenerateDigestManifest>?,
    projectName: String,
    resourcePath: String,
    verify: TaskProvider<VerifyNativePlatforms>,
    buildTaskName: String
): TaskProvider<Jar> =
    tasks.register<Jar>(JniTaskNames.nativeJar(source.platformId)) {
        group = KreateTaskGroup.JNI.label
        description = "Packages the ${source.platformId} native library of $projectName."

        dependsOn(verify)
        if (source.hostBuilt != null) {
            dependsOn(buildTaskName)
        }

        source.directoriesByPrecedence().forEach { directory ->
            from(directory) {
                into("$resourcePath/${source.platformId}")
                exclude(STAGE_MANIFEST_FILE_NAME)
            }
        }
        if (digests != null) {
            from(digests) { into(resourcePath) }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveBaseName.set(projectName)
        archiveAppendix.set(source.platformId)
        destinationDirectory.set(layout.buildDirectory.dir(NATIVE_JAR_DIRECTORY))
    }
