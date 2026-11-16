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
import org.gradle.api.file.Directory
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.util.Properties

private const val VERSION_PROPERTY: String = "version"

private const val COMPOSITE_PROPERTIES_PATH: String = "../gradle.properties"

private const val RELEASE_TAG_VARIABLE: String = "CI_COMMIT_TAG"

private const val TAG_PREFIX: String = "v"

public fun publishedVersion(providers: ProviderFactory, settingsDirectory: Directory): String {
    val declared = compositeVersion(providers, settingsDirectory)
    val tag = releaseTag(providers) ?: return snapshotOf(declared)

    val released = tag.removePrefix(TAG_PREFIX)
    if (released.endsWith(SNAPSHOT_SUFFIX)) throw snapshotTag(tag)
    if (released != declared) throw mismatchedTag(tag, declared)
    return released
}

public fun compositeVersion(providers: ProviderFactory, settingsDirectory: Directory): String {
    val propertiesFile = settingsDirectory.file(COMPOSITE_PROPERTIES_PATH)
    val location = propertiesFile.asFile
    val fileContents = providers.fileContents(propertiesFile)
    val contents = fileContents.asText.orNull ?: throw unreadableVersion(location)

    val properties = Properties()
    contents.reader().use { reader -> properties.load(reader) }
    val declared = properties.getProperty(VERSION_PROPERTY)?.trim()
    if (declared.isNullOrEmpty()) throw missingVersion(location)
    return declared
}

private fun releaseTag(providers: ProviderFactory): String? {
    val tag = providers.environmentVariable(RELEASE_TAG_VARIABLE).orNull
    return tag?.takeIf { it.isNotBlank() }
}

private fun snapshotOf(declared: String): String {
    if (declared.endsWith(SNAPSHOT_SUFFIX)) return declared
    return "$declared$SNAPSHOT_SUFFIX"
}

private fun snapshotTag(tag: String): GradleException = GradleException(
    "Kreate refuses to release from tag '$tag': a release tag never carries the " +
        "'$SNAPSHOT_SUFFIX' suffix. Snapshots are published from the develop branch, without a tag."
)

private fun mismatchedTag(tag: String, declared: String): GradleException = GradleException(
    "Kreate refuses to release from tag '$tag': the composite root declares " +
        "'$VERSION_PROPERTY=$declared'. Tag the commit whose gradle.properties carries the released version."
)

private fun unreadableVersion(location: File): GradleException = GradleException(
    "Kreate could not read the composite root's version: '${location.absolutePath}' does not exist " +
        "or cannot be read. That file is the single source of truth for the version of the published plugin."
)

private fun missingVersion(location: File): GradleException = GradleException(
    "Kreate could not read the composite root's version: '${location.absolutePath}' declares no " +
        "'$VERSION_PROPERTY' property. Add one, for example '$VERSION_PROPERTY=4.0.0'."
)
