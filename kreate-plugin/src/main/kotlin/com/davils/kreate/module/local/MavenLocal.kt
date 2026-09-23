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
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The system property naming the user's home directory.
 *
 * @since 3.2.0
 */
private const val USER_HOME_PROPERTY: String = "user.home"

/**
 * Resolves the local Maven repository from a build's provider factory.
 *
 * Reads both inputs through providers so that they are recorded as configuration cache inputs. A
 * build that cached a repository location and then had `maven.repo.local` changed under it would
 * keep publishing where the old entry said, which is precisely the kind of silent wrong answer
 * this feature cannot afford.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @return The directory holding the local Maven repository, which may not exist yet.
 * @since 3.2.0
 */
internal fun mavenLocalOf(providers: ProviderFactory): File {
    val configured = providers.systemProperty(MAVEN_REPO_LOCAL_PROPERTY).orNull
    val userHome = File(providers.systemProperty(USER_HOME_PROPERTY).getOrElse(""))

    return resolveMavenLocal(
        systemProperties = configured?.let { mapOf(MAVEN_REPO_LOCAL_PROPERTY to it) }.orEmpty(),
        userHome = userHome
    )
}

/**
 * The system property Maven and Gradle both read to relocate the local repository.
 *
 * @since 3.2.0
 */
internal const val MAVEN_REPO_LOCAL_PROPERTY: String = "maven.repo.local"

/**
 * The default location of the local Maven repository, relative to the user's home.
 *
 * @since 3.2.0
 */
private const val DEFAULT_REPOSITORY_PATH: String = ".m2/repository"

/**
 * The Maven settings file that may relocate the local repository.
 *
 * @since 3.2.0
 */
private const val MAVEN_SETTINGS_PATH: String = ".m2/settings.xml"

/**
 * The element of `settings.xml` naming the local repository.
 *
 * @since 3.2.0
 */
private const val LOCAL_REPOSITORY_ELEMENT: String = "localRepository"

/**
 * Resolves the local Maven repository.
 *
 * Follows the same order as Gradle's own `mavenLocal()`, because a build that publishes to one
 * directory and resolves from another is the kind of failure that looks like the feature simply
 * not working: the `maven.repo.local` system property, then `<localRepository>` in
 * `~/.m2/settings.xml`, then `~/.m2/repository`.
 *
 * The system property coming first is also what makes the functional tests hermetic — they point
 * it at a temporary directory so the suite never touches a developer's real repository.
 *
 * @param systemProperties The system properties to read, injectable so tests do not have to
 * mutate the JVM's.
 * @param userHome The user's home directory.
 * @return The directory holding the local Maven repository, which may not exist yet.
 * @since 3.2.0
 */
internal fun resolveMavenLocal(
    systemProperties: Map<String, String>,
    userHome: File
): File {
    val fromProperty = systemProperties[MAVEN_REPO_LOCAL_PROPERTY]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { configured -> File(configured).absoluteFile }

    return fromProperty
        ?: localRepositoryFromSettings(File(userHome, MAVEN_SETTINGS_PATH), userHome)
        ?: File(userHome, DEFAULT_REPOSITORY_PATH)
}

/**
 * Reads `<localRepository>` out of a Maven settings file.
 *
 * A malformed or unreadable settings file yields `null` rather than an error. Maven's settings
 * are not Kreate's to validate, and refusing to configure the build because of a file this
 * feature merely consults would be an overreach.
 *
 * @param settings The settings file, which may not exist.
 * @param userHome The user's home directory, used to expand a leading `~`.
 * @return The configured directory, or `null` if the file declares none.
 * @since 3.2.0
 */
private fun localRepositoryFromSettings(settings: File, userHome: File): File? {
    if (!settings.isFile) return null

    val configured = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // The file is the user's own, but an XML parser that resolves external entities is a
            // liability wherever it appears and costs nothing to switch off here.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }

        factory.newDocumentBuilder()
            .parse(settings)
            .getElementsByTagName(LOCAL_REPOSITORY_ELEMENT)
            .takeIf { it.length > 0 }
            ?.item(0)
            ?.textContent
            ?.trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    return configured?.let { path ->
        if (path.startsWith(HOME_PREFIX)) {
            File(userHome, path.removePrefix(HOME_PREFIX))
        } else {
            File(path)
        }.absoluteFile
    }
}

/**
 * The prefix Maven users write for a path under their home directory.
 *
 * @since 3.2.0
 */
private const val HOME_PREFIX: String = "~/"

/**
 * Returns the directory a module version occupies in a Maven repository.
 *
 * @param repository The repository root.
 * @param module The module coordinate.
 * @param version The version.
 * @return The directory, which may not exist.
 * @since 3.2.0
 */
internal fun moduleDirectory(repository: File, module: LocalModule, version: String): File =
    File(groupDirectory(repository, module.group), "${module.name}/$version")

/**
 * Returns the directory a group occupies in a Maven repository.
 *
 * @param repository The repository root.
 * @param group The group.
 * @return The directory, which may not exist.
 * @since 3.2.0
 */
internal fun groupDirectory(repository: File, group: String): File =
    File(repository, group.replace('.', '/'))

/**
 * Returns the coordinates a library claims to have published that are not actually installed.
 *
 * The state file is written after `publishToMavenLocal` succeeds, so a gap here means something
 * removed the artefacts afterwards — most often a `kreateLocalClean` in another shell, or a
 * hand-run `rm` on `~/.m2`. Without this check the build would instead fail much later with a
 * "could not find" that names a version nobody typed.
 *
 * @param repository The local Maven repository root.
 * @param library The recorded library.
 * @return The missing coordinates, empty when everything is present.
 * @since 3.2.0
 */
internal fun missingArtifacts(repository: File, library: LocalLibrary): List<String> =
    library.modules
        .filterNot { module -> moduleDirectory(repository, module, library.version).isDirectory }
        .map { module -> "${module.coordinate}:${library.version}" }

/**
 * Returns every locally published version directory under a group.
 *
 * Only directories whose name carries [SNAPSHOT_SUFFIX] are returned. A release that happens to
 * sit in the same repository was put there by something other than this feature — a Maven build,
 * or a `publishToMavenLocal` predating it — and deleting it is not this task's business.
 *
 * The walk is not depth limited, because a Maven layout nests a group inside its own artefact's
 * directory whenever one is a prefix of the other. Kreate is exactly that case: `com.davils:kreate`
 * sits at `com/davils/kreate/<version>` while the marker
 * `com.davils.kreate:com.davils.kreate.gradle.plugin` sits one level below it. A limit chosen for
 * the ordinary layout would silently leave the marker behind, and a stale marker is worse than no
 * clean at all — it pins a plugin version that no longer has an artefact.
 *
 * A candidate has to contain at least one file to count, so an artefact that happens to be named
 * like a snapshot is not mistaken for a version.
 *
 * @param repository The local Maven repository root.
 * @param group The group to scan.
 * @return The snapshot version directories, ordered by path.
 * @since 3.2.0
 */
internal fun snapshotDirectories(repository: File, group: String): List<File> {
    val root = groupDirectory(repository, group)
    if (!root.isDirectory) return emptyList()

    return root.walkTopDown()
        .filter { candidate ->
            candidate.isDirectory &&
                candidate.name.endsWith(SNAPSHOT_SUFFIX) &&
                candidate.listFiles()?.any { it.isFile } == true
        }
        .sortedBy { it.absolutePath }
        .toList()
}
