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

import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Properties

/**
 * The directory under the Gradle user home that records what is currently published locally.
 *
 * The Gradle user home rather than a file in the repository, and that choice is the feature's
 * main safety property rather than a matter of taste. GitLab's shared pipeline points
 * `GRADLE_USER_HOME` at `$CI_PROJECT_DIR/.gradle`, which is created fresh for every job, so this
 * directory cannot exist in CI. A file in the repository would instead be one `git add -A` away
 * from turning a developer's local state into everyone's.
 *
 * It also sits next to `gradle.properties`, which is already where a developer keeps the GitLab
 * token — the same trust boundary, and the same "never in a repository" property.
 *
 * @since 3.2.0
 */
internal const val STATE_DIRECTORY: String = "davils/local"

/**
 * The extension of a state file.
 *
 * @since 3.2.0
 */
internal const val STATE_EXTENSION: String = ".properties"

/**
 * The Gradle property that relocates the state directory.
 *
 * Intended for two situations and no others: Kreate's own functional tests, which drive a
 * producer build and a consumer build against a shared directory and must not touch the
 * developer's real one, and a developer who runs several Gradle user homes and wants one
 * workspace across them.
 *
 * It does not weaken the CI guarantee. Local mode is switched off by the CI environment
 * variables regardless of where the state lives, and a runner that carries state fails whichever
 * directory it was found in.
 *
 * @since 3.2.0
 */
internal const val STATE_DIRECTORY_PROPERTY: String = "davils.local.state.dir"

/**
 * Resolves the directory holding the local development state.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @param gradleUserHome The Gradle user home, used unless the override is set.
 * @return The state directory, which may not exist.
 * @since 3.2.0
 */
internal fun stateDirectoryOf(providers: ProviderFactory, gradleUserHome: File): File =
    providers.gradleProperty(STATE_DIRECTORY_PROPERTY)
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { configured -> File(configured).absoluteFile }
        ?: File(gradleUserHome, STATE_DIRECTORY)

private const val KEY_LIBRARY: String = "library"
private const val KEY_GROUP: String = "group"
private const val KEY_REPOSITORY: String = "repository"
private const val KEY_VERSION: String = "version"
private const val KEY_PUBLISHED_AT: String = "publishedAt"
private const val KEY_KREATE: String = "kreate"
private const val KEY_MODULES: String = "modules"

/**
 * A single locally published library.
 *
 * @since 3.2.0
 */
internal data class LocalLibrary(
    /**
     * The name the state file is keyed by, normally the producer's root project name.
     * @since 3.2.0
     */
    val library: String,
    /**
     * The group of the published artefacts.
     * @since 3.2.0
     */
    val group: String,
    /**
     * The working copy the publication was built from.
     * @since 3.2.0
     */
    val repository: File,
    /**
     * The published version, always carrying [SNAPSHOT_SUFFIX].
     * @since 3.2.0
     */
    val version: String,
    /**
     * When the publication happened, as an ISO-8601 instant, or `null` if the record has none.
     *
     * Deliberately text rather than an [Instant]. This type crosses the configuration cache as
     * part of a `ValueSource` result, and Gradle 9.0 — the declared minimum — falls back to Java
     * serialization for `java.time` types and then fails, because `java.base` does not open
     * `java.time` to the unnamed module. Parse it where it is displayed.
     *
     * @since 3.2.0
     */
    val publishedAt: String?,
    /**
     * The version of Kreate that produced the publication.
     * @since 3.2.0
     */
    val kreateVersion: String,
    /**
     * The coordinates the publication installed.
     * @since 3.2.0
     */
    val modules: List<LocalModule>
) : Serializable {
    internal companion object {
        /**
         * The serial version identifier.
         * @since 3.2.0
         */
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Everything that is currently published to the local Maven repository.
 *
 * Serializable because the settings plugin hands it to `GradleLifecycle.beforeProject`, whose
 * `IsolatedAction` may only capture data.
 *
 * @since 3.2.0
 */
internal data class LocalWorkspace(
    /**
     * The recorded libraries, ordered by name.
     * @since 3.2.0
     */
    val libraries: List<LocalLibrary>
) : Serializable {
    /**
     * Whether nothing is published locally.
     *
     * @since 3.2.0
     */
    val isEmpty: Boolean get() = libraries.isEmpty()

    /**
     * The version to substitute, keyed by `group:name`.
     *
     * A coordinate published by two libraries at once would be a mistake on the producer side;
     * the later entry wins and [duplicateCoordinates] names the overlap so the build can say so.
     *
     * @since 3.2.0
     */
    val substitutions: Map<String, String> by lazy {
        libraries.flatMap { library ->
            library.modules.map { module -> module.coordinate to library.version }
        }.toMap()
    }

    /**
     * Coordinates claimed by more than one library.
     *
     * @since 3.2.0
     */
    val duplicateCoordinates: List<String> by lazy {
        libraries
            .flatMap { library -> library.modules.map { module -> module.coordinate } }
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .sorted()
    }

    internal companion object {
        /**
         * The serial version identifier.
         * @since 3.2.0
         */
        private const val serialVersionUID: Long = 1L

        /**
         * A workspace with nothing published.
         *
         * @since 3.2.0
         */
        val EMPTY: LocalWorkspace = LocalWorkspace(emptyList())
    }
}

/**
 * Returns a copy of this workspace holding only the named libraries.
 *
 * An extension rather than a member because a data class exists to carry values, and detekt
 * enforces that here.
 *
 * @param names The library names to keep.
 * @return The narrowed workspace.
 * @since 3.2.0
 */
internal fun LocalWorkspace.restrictedTo(names: Set<String>): LocalWorkspace =
    LocalWorkspace(libraries.filter { it.library in names })

/**
 * Reads every state file in [directory].
 *
 * A file that cannot be parsed is skipped rather than failing the build. The state directory is
 * machine-global and outside version control, so a half-written or hand-edited file there is a
 * plausible accident; refusing to configure any build until someone finds it would turn a small
 * mess into a blocked machine. What must not be skipped silently is a *recorded* library whose
 * artefacts are missing, and that is checked separately against the Maven repository.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @return The libraries currently published locally, ordered by name.
 * @since 3.2.0
 */
internal fun readLocalWorkspace(directory: File): LocalWorkspace {
    val files = directory.listFiles { file -> file.isFile && file.name.endsWith(STATE_EXTENSION) }
        ?: return LocalWorkspace.EMPTY

    return LocalWorkspace(files.mapNotNull(::readLocalLibrary).sortedBy { it.library })
}

/**
 * Reads a single state file.
 *
 * @param file The file to read.
 * @return The recorded library, or `null` if the file is unreadable or incomplete.
 * @since 3.2.0
 */
private fun readLocalLibrary(file: File): LocalLibrary? {
    val properties = runCatching {
        Properties().apply { file.reader(StandardCharsets.UTF_8).use { reader -> load(reader) } }
    }.getOrNull() ?: return null

    return buildLocalLibrary(properties)
}

/**
 * Builds a library record out of a parsed state file.
 *
 * @param properties The parsed file.
 * @return The recorded library, or `null` if a required value is missing.
 * @since 3.2.0
 */
private fun buildLocalLibrary(properties: Properties): LocalLibrary? {
    fun value(key: String): String? =
        properties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

    val library = value(KEY_LIBRARY).orEmpty()
    val group = value(KEY_GROUP).orEmpty()
    val version = value(KEY_VERSION).orEmpty()
    val modules = value(KEY_MODULES)?.split(',')?.mapNotNull(LocalModule::parse).orEmpty()

    // A record missing a required key, or naming no modules, substitutes nothing — it is
    // indistinguishable from absence except that it would make the build announce local mode is
    // on and then change nothing. Treat it as absence.
    val incomplete = library.isEmpty() || group.isEmpty() || version.isEmpty()
    if (incomplete || modules.isEmpty()) return null

    return LocalLibrary(
        library = library,
        group = group,
        repository = File(value(KEY_REPOSITORY).orEmpty()),
        version = version,
        publishedAt = value(KEY_PUBLISHED_AT),
        kreateVersion = value(KEY_KREATE).orEmpty(),
        modules = modules
    )
}

/**
 * Records a locally published build.
 *
 * Written whole to a sibling temporary file and then moved into place. Two repositories can be
 * published at the same time — `kreateLocalPublishAll` runs them in sequence, but nothing stops a
 * developer running two shells — and a consumer configuring in between must never observe a
 * half-written file.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @param library The library to record.
 * @return The file that was written.
 * @throws IllegalArgumentException If the version does not carry [SNAPSHOT_SUFFIX].
 * @since 3.2.0
 */
internal fun writeLocalLibrary(directory: File, library: LocalLibrary): File {
    require(library.version.endsWith(SNAPSHOT_SUFFIX)) {
        "Refusing to record '${library.group}:${library.library}:${library.version}' as a local " +
            "build: a local publication has to carry the '$SNAPSHOT_SUFFIX' suffix so that it " +
            "can never shadow a release."
    }

    Files.createDirectories(directory.toPath())

    val contents = Properties().apply {
        setProperty(KEY_LIBRARY, library.library)
        setProperty(KEY_GROUP, library.group)
        setProperty(KEY_REPOSITORY, library.repository.absolutePath)
        setProperty(KEY_VERSION, library.version)
        setProperty(KEY_PUBLISHED_AT, library.publishedAt ?: Instant.now().toString())
        setProperty(KEY_KREATE, library.kreateVersion)
        setProperty(KEY_MODULES, library.modules.map { it.coordinate }.sorted().joinToString(","))
    }

    val target = stateFileOf(directory, library.group, library.library)
    val temporary = File(target.parentFile, "${target.name}.tmp")

    // A Writer rather than an OutputStream: the stream form escapes everything outside Latin-1,
    // and the repository path is the one value here that can legitimately contain anything.
    temporary.writer(StandardCharsets.UTF_8).use { writer ->
        contents.store(writer, "Written by kreateLocalPublish. Delete with kreateLocalClean.")
    }
    Files.move(
        temporary.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
    )

    return target
}

/**
 * Returns the state file a library is recorded in.
 *
 * One file per library rather than one shared file, so that concurrent publishes cannot corrupt
 * each other and so that removing a single library is a file deletion.
 *
 * @param directory The state directory, as resolved by [stateDirectoryOf].
 * @param group The group of the published artefacts.
 * @param library The library name.
 * @return The file, which may not exist.
 * @since 3.2.0
 */
internal fun stateFileOf(directory: File, group: String, library: String): File =
    File(directory, "$group.$library$STATE_EXTENSION")

/**
 * Parses a recorded timestamp.
 *
 * @return The instant, or `null` if the record carries none or one that cannot be read.
 * @since 3.2.0
 */
internal fun LocalLibrary.publishedAtInstant(): Instant? =
    publishedAt?.let { recorded -> runCatching { Instant.parse(recorded) }.getOrNull() }
