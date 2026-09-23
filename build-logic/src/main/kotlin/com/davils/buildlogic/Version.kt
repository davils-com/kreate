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
import java.util.Properties

/**
 * The name of the property that carries the version in `gradle.properties`.
 *
 * @since 3.2.0
 */
private const val VERSION_PROPERTY: String = "version"

/**
 * The path from an included build's settings directory to the composite root's properties.
 *
 * @since 3.2.0
 */
private const val COMPOSITE_PROPERTIES_PATH: String = "../gradle.properties"

/**
 * Reads the project version from the composite root's `gradle.properties`.
 *
 * `kreate-plugin` is an included build, and Gradle does not propagate the properties of the
 * build that includes it. Until 3.2.0 nothing bridged that gap, so the plugin's version was
 * `unspecified` in every invocation that did not set `CI_COMMIT_TAG` or pass `-Pversion=`
 * explicitly — which is how a `2.2.0-SNAPSHOT` came to be the only thing anyone ever
 * installed into a local Maven repository by hand.
 *
 * The fix reads the root's file rather than adding a second `gradle.properties` next to the
 * included build. Two files naming the same version is the failure mode, not the remedy:
 * they agree until the first release that forgets one of them.
 *
 * The result is a plain `String` rather than a `Provider`, because `version` has to be
 * assigned during script evaluation for `coordinates(...)` to see it. Reading through
 * [ProviderFactory.fileContents] still registers the file as a configuration cache input,
 * so a version bump invalidates the cache.
 *
 * @param providers The provider factory of the surrounding project.
 * @param settingsDirectory The settings directory of the included build, whose parent holds
 * the composite root's `gradle.properties`.
 * @return The version declared by the composite root.
 * @throws GradleException If the file is missing, unreadable, or declares no version.
 * @since 3.2.0
 */
public fun compositeVersion(providers: ProviderFactory, settingsDirectory: Directory): String {
    val propertiesFile = settingsDirectory.file(COMPOSITE_PROPERTIES_PATH)
    val contents = providers.fileContents(propertiesFile).asText.orNull
        ?: throw GradleException(
            "Kreate could not read the composite root's version: " +
                "'${propertiesFile.asFile.absolutePath}' does not exist or cannot be read. " +
                "That file is the single source of truth for the version of the published plugin."
        )

    val declared = Properties()
        .apply { contents.reader().use { reader -> load(reader) } }
        .getProperty(VERSION_PROPERTY)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    return declared ?: throw GradleException(
        "Kreate could not read the composite root's version: " +
            "'${propertiesFile.asFile.absolutePath}' declares no '$VERSION_PROPERTY' property. " +
            "Add one, for example '$VERSION_PROPERTY=3.2.0'."
    )
}
