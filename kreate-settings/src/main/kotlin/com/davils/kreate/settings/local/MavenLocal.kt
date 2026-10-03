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
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * The system property Maven and Gradle both read to relocate the local repository.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val MAVEN_REPO_LOCAL_PROPERTY: String = "maven.repo.local"

private const val USER_HOME_PROPERTY: String = "user.home"

private const val DEFAULT_REPOSITORY_PATH: String = ".m2/repository"

private const val MAVEN_SETTINGS_PATH: String = ".m2/settings.xml"

private const val LOCAL_REPOSITORY_ELEMENT: String = "localRepository"

private const val DISALLOW_DOCTYPE_FEATURE: String = "http://apache.org/xml/features/disallow-doctype-decl"

private const val HOME_PREFIX: String = "~/"

private const val FIRST_ELEMENT: Int = 0

private const val GROUP_SEPARATOR: Char = '.'

private const val PATH_SEPARATOR: Char = '/'

private val logger: Logger = Logging.getLogger(LocalWorkspace::class.java)

/**
 * Resolves the local Maven repository from a build's provider factory.
 *
 * Reads both inputs through providers so that they are recorded as configuration cache inputs,
 * and a changed `maven.repo.local` is never answered from a stale cache entry.
 *
 * @param providers The provider factory of the surrounding `Project` or `Settings`.
 * @return The directory holding the local Maven repository, which may not exist yet.
 * @since 3.2.0
 */
@InternalKreateApi
public fun mavenLocalOf(providers: ProviderFactory): File {
    val configured = providers.systemProperty(MAVEN_REPO_LOCAL_PROPERTY).orNull
    val userHome = providers.systemProperty(USER_HOME_PROPERTY).getOrElse("")
    val systemProperties = configured?.let { mapOf(MAVEN_REPO_LOCAL_PROPERTY to it) }.orEmpty()

    return resolveMavenLocal(systemProperties = systemProperties, userHome = File(userHome))
}

/**
 * Resolves the local Maven repository.
 *
 * Follows the same order as Gradle's own `mavenLocal()`, so that a build never publishes to one
 * directory and resolves from another: the `maven.repo.local` system property, then
 * `<localRepository>` in `~/.m2/settings.xml`, then `~/.m2/repository`.
 *
 * @param systemProperties The system properties to read, injectable so tests do not have to
 * mutate the JVM's.
 * @param userHome The user's home directory.
 * @return The directory holding the local Maven repository, which may not exist yet.
 * @since 3.2.0
 */
@InternalKreateApi
public fun resolveMavenLocal(
    systemProperties: Map<String, String>,
    userHome: File
): File {
    val fromProperty = systemProperties[MAVEN_REPO_LOCAL_PROPERTY].trimmedOrNull()
    if (fromProperty != null) return File(fromProperty).absoluteFile

    return localRepositoryFromSettings(File(userHome, MAVEN_SETTINGS_PATH), userHome)
        ?: File(userHome, DEFAULT_REPOSITORY_PATH)
}

private fun localRepositoryFromSettings(settings: File, userHome: File): File? {
    if (!settings.isFile) return null

    val configured = readLocalRepositoryElement(settings) ?: return null
    return expandHome(configured, userHome).absoluteFile
}

private fun readLocalRepositoryElement(settings: File): String? =
    try {
        localRepositoryElementOf(settings)
    } catch (exception: IOException) {
        skipUnreadableSettings(settings, exception)
    } catch (exception: SAXException) {
        skipUnreadableSettings(settings, exception)
    } catch (exception: ParserConfigurationException) {
        skipUnreadableSettings(settings, exception)
    }

private fun localRepositoryElementOf(settings: File): String? {
    val document = entityFreeDocumentBuilder().parse(settings)
    val elements = document.getElementsByTagName(LOCAL_REPOSITORY_ELEMENT)
    if (elements.length == 0) return null

    val element = elements.item(FIRST_ELEMENT)
    return element?.textContent.trimmedOrNull()
}

private fun entityFreeDocumentBuilder(): DocumentBuilder {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setFeature(DISALLOW_DOCTYPE_FEATURE, true)
    factory.isXIncludeAware = false
    factory.isExpandEntityReferences = false
    return factory.newDocumentBuilder()
}

private fun skipUnreadableSettings(settings: File, cause: Exception): String? {
    logger.info("Kreate ignores the Maven settings $settings, which cannot be read.", cause)
    return null
}

private fun expandHome(path: String, userHome: File): File {
    if (!path.startsWith(HOME_PREFIX)) return File(path)

    return File(userHome, path.removePrefix(HOME_PREFIX))
}

/**
 * Returns the directory a module version occupies in a Maven repository.
 *
 * @param repository The repository root.
 * @param module The module coordinate.
 * @param version The version.
 * @return The directory, which may not exist.
 * @since 3.2.0
 */
@InternalKreateApi
public fun moduleDirectory(repository: File, module: LocalModule, version: String): File =
    File(groupDirectory(repository, module.group), "${module.name}$PATH_SEPARATOR$version")

/**
 * Returns the directory a group occupies in a Maven repository.
 *
 * @param repository The repository root.
 * @param group The group.
 * @return The directory, which may not exist.
 * @since 3.2.0
 */
@InternalKreateApi
public fun groupDirectory(repository: File, group: String): File =
    File(repository, group.replace(GROUP_SEPARATOR, PATH_SEPARATOR))

/**
 * Returns the coordinates a library claims to have published that are not actually installed.
 *
 * The state file is written after `publishToMavenLocal` succeeds, so a gap here means something
 * removed the artefacts afterwards, most often a `kreateLocalClean` in another shell.
 *
 * @param repository The local Maven repository root.
 * @param library The recorded library.
 * @return The missing coordinates, empty when everything is present.
 * @since 3.2.0
 */
@InternalKreateApi
public fun missingArtifacts(repository: File, library: LocalLibrary): List<String> =
    library.modules
        .filterNot { module -> moduleDirectory(repository, module, library.version).isDirectory }
        .map { module -> "${module.coordinate}:${library.version}" }

/**
 * Returns every locally published version directory under a group.
 *
 * Only directories whose name carries [SNAPSHOT_SUFFIX] and that contain at least one file are
 * returned, so releases and artefacts merely named like a snapshot are left alone.
 *
 * The walk is not depth limited, because a Maven layout nests a group inside its own artefact's
 * directory whenever one is a prefix of the other: Kreate's own plugin marker lives inside the
 * `com/davils/kreate` directory of its artefact, and a stale marker pins a plugin version that no
 * longer exists.
 *
 * @param repository The local Maven repository root.
 * @param group The group to scan.
 * @return The snapshot version directories, ordered by path.
 * @since 3.2.0
 */
@InternalKreateApi
public fun snapshotDirectories(repository: File, group: String): List<File> {
    val root = groupDirectory(repository, group)
    if (!root.isDirectory) return emptyList()

    val candidates = root.walkTopDown().filter { candidate -> candidate.isSnapshotVersionDirectory() }
    return candidates.sortedBy { it.absolutePath }.toList()
}

private fun File.isSnapshotVersionDirectory(): Boolean {
    val isNamedLikeSnapshot = isDirectory && name.endsWith(SNAPSHOT_SUFFIX)
    if (!isNamedLikeSnapshot) return false

    return listFiles()?.any { it.isFile } == true
}
