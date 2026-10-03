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

package com.davils.kreate.settings.local

import com.davils.kreate.settings.InternalKreateApi
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Properties

/**
 * The directory under the Gradle user home that records what is currently published locally.
 *
 * The Gradle user home rather than a file in the repository is the feature's main safety
 * property. A CI pipeline that points `GRADLE_USER_HOME` inside its own workspace recreates this
 * directory empty for every job, so it cannot carry local state between jobs or from a
 * developer's machine, and the state can never be committed by accident.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val STATE_DIRECTORY: String = "kreate/local"

/**
 * The extension of a state file.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val STATE_EXTENSION: String = ".properties"

/**
 * The Gradle property that relocates the state directory.
 *
 * Intended for Kreate's own functional tests and for a developer who runs several Gradle user
 * homes and wants one workspace across them. It does not weaken the CI guarantee, which holds
 * wherever the state lives.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val STATE_DIRECTORY_PROPERTY: String = "kreate.local.state.dir"

private const val KEY_LIBRARY: String = "library"
private const val KEY_GROUP: String = "group"
private const val KEY_REPOSITORY: String = "repository"
private const val KEY_VERSION: String = "version"
private const val KEY_PUBLISHED_AT: String = "publishedAt"
private const val KEY_KREATE: String = "kreate"
private const val KEY_MODULES: String = "modules"

private const val MODULE_SEPARATOR: String = ","

private const val TEMPORARY_SUFFIX: String = ".tmp"

private const val STATE_FILE_HEADER: String =
    "Written by ${LocalTaskNames.PUBLISH}. Delete with ${LocalTaskNames.CLEAN}."

private val logger: Logger = Logging.getLogger(LocalWorkspace::class.java)

/**
 * Resolves the directory holding the local development state.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @param gradleUserHome The Gradle user home, used unless the override is set.
 * @return The state directory, which may not exist.
 * @since 3.2.0
 */
@InternalKreateApi
public fun stateDirectoryOf(providers: ProviderFactory, gradleUserHome: File): File {
    val configured = providers.gradleProperty(STATE_DIRECTORY_PROPERTY).orNull
    val relocated = configured.trimmedOrNull() ?: return File(gradleUserHome, STATE_DIRECTORY)

    return File(relocated).absoluteFile
}

/**
 * Reads every state file in [directory].
 *
 * A file that cannot be parsed is skipped rather than failing the build, because a half-written
 * or hand-edited file in a machine-global directory must not block every build on the machine.
 * Recorded libraries whose artefacts are missing are checked separately, against the Maven
 * repository.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @return The libraries currently published locally, ordered by name.
 * @since 3.2.0
 */
@InternalKreateApi
public fun readLocalWorkspace(directory: File): LocalWorkspace {
    val files = directory.listFiles { file -> file.isFile && file.name.endsWith(STATE_EXTENSION) }
        ?: return LocalWorkspace.EMPTY

    return LocalWorkspace(files.mapNotNull(::readLocalLibrary).sortedBy { it.library })
}

private fun readLocalLibrary(file: File): LocalLibrary? {
    val properties = loadProperties(file) ?: return null

    return buildLocalLibrary(properties)
}

private fun loadProperties(file: File): Properties? =
    try {
        Properties().apply { file.reader(StandardCharsets.UTF_8).use { reader -> load(reader) } }
    } catch (exception: IOException) {
        skipUnreadable(file, exception)
    } catch (exception: IllegalArgumentException) {
        skipUnreadable(file, exception)
    }

private fun skipUnreadable(file: File, cause: Exception): Properties? {
    logger.info("Kreate skips the local state file $file, which cannot be read.", cause)
    return null
}

private fun buildLocalLibrary(properties: Properties): LocalLibrary? {
    val library = properties.valueOf(KEY_LIBRARY).orEmpty()
    val group = properties.valueOf(KEY_GROUP).orEmpty()
    val version = properties.valueOf(KEY_VERSION).orEmpty()
    val modules = recordedModules(properties)

    val substitutesNothing = library.isEmpty() || group.isEmpty() || version.isEmpty() || modules.isEmpty()
    if (substitutesNothing) return null

    return LocalLibrary(
        library = library,
        group = group,
        repository = File(properties.valueOf(KEY_REPOSITORY).orEmpty()),
        version = version,
        publishedAt = properties.valueOf(KEY_PUBLISHED_AT),
        kreateVersion = properties.valueOf(KEY_KREATE).orEmpty(),
        modules = modules
    )
}

private fun recordedModules(properties: Properties): List<LocalModule> {
    val recorded = properties.valueOf(KEY_MODULES)
    val coordinates = recorded?.split(MODULE_SEPARATOR).orEmpty()
    return coordinates.mapNotNull(LocalModule::parse)
}

private fun Properties.valueOf(key: String): String? = getProperty(key).trimmedOrNull()

/**
 * Records a locally published build.
 *
 * Written whole to a sibling temporary file and then moved into place, so that a consumer
 * configuring while a publish runs in another shell never observes a half-written file.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @param library The library to record.
 * @return The file that was written.
 * @throws IllegalArgumentException If the version does not carry [SNAPSHOT_SUFFIX].
 * @since 3.2.0
 */
@InternalKreateApi
public fun writeLocalLibrary(directory: File, library: LocalLibrary): File {
    require(library.version.endsWith(SNAPSHOT_SUFFIX)) {
        "Refusing to record '${library.group}:${library.library}:${library.version}' as a local " +
            "build: a local publication has to carry the '$SNAPSHOT_SUFFIX' suffix so that it " +
            "can never shadow a release."
    }

    Files.createDirectories(directory.toPath())

    val target = stateFileOf(directory, library.group, library.library)
    val temporary = File(target.parentFile, "${target.name}$TEMPORARY_SUFFIX")

    stateOf(library).storeAsUtf8(temporary)
    Files.move(
        temporary.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
    )

    return target
}

private fun stateOf(library: LocalLibrary): Properties = Properties().apply {
    setProperty(KEY_LIBRARY, library.library)
    setProperty(KEY_GROUP, library.group)
    setProperty(KEY_REPOSITORY, library.repository.absolutePath)
    setProperty(KEY_VERSION, library.version)
    setProperty(KEY_PUBLISHED_AT, library.publishedAt ?: Instant.now().toString())
    setProperty(KEY_KREATE, library.kreateVersion)
    setProperty(KEY_MODULES, moduleList(library))
}

private fun moduleList(library: LocalLibrary): String {
    val coordinates = library.modules.map { it.coordinate }
    return coordinates.sorted().joinToString(MODULE_SEPARATOR)
}

private fun Properties.storeAsUtf8(file: File) {
    file.writer(StandardCharsets.UTF_8).use { writer -> store(writer, STATE_FILE_HEADER) }
}

/**
 * Returns the state file a library is recorded in.
 *
 * One file per library, so that concurrent publishes cannot corrupt each other and removing a
 * single library is a file deletion.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @param group The group of the published artefacts.
 * @param library The library name.
 * @return The file, which may not exist.
 * @since 3.2.0
 */
@InternalKreateApi
public fun stateFileOf(directory: File, group: String, library: String): File =
    File(directory, "$group.$library$STATE_EXTENSION")
