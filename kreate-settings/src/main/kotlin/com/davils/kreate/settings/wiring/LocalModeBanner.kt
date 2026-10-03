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

package com.davils.kreate.settings.wiring

import com.davils.kreate.settings.local.LOCAL_PROPERTY
import com.davils.kreate.settings.local.LocalLibrary
import com.davils.kreate.settings.local.LocalMode
import com.davils.kreate.settings.local.LocalTaskNames
import com.davils.kreate.settings.local.publishedAtInstant
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.time.Duration
import java.time.Instant

private const val NAME_WIDTH: Int = 14

private const val VERSION_WIDTH: Int = 20

private const val AGE_WIDTH: Int = 14

private const val SINGLE_LIBRARY: Int = 1

internal fun Settings.announceLocalMode(mode: LocalMode.Active, repository: File) {
    val logger = Logging.getLogger(Settings::class.java)
    val libraries = mode.workspace.libraries

    logger.lifecycle("")
    logger.lifecycle(
        "Kreate local mode is ON — ${libraries.size} ${librariesNoun(libraries.size)} " +
            "resolved from $repository:"
    )
    libraries.forEach { library -> logger.lifecycle(render(library)) }

    warnAboutDuplicates(logger, mode.workspace.duplicateCoordinates)

    logger.lifecycle("  Dependency locking is off. No lock file is read or written.")
    logger.lifecycle(
        "  Switch off with -P$LOCAL_PROPERTY=false; clear with ./gradlew ${LocalTaskNames.CLEAN}."
    )
    logger.lifecycle("")
}

private fun librariesNoun(count: Int): String {
    if (count == SINGLE_LIBRARY) return "library"

    return "libraries"
}

private fun warnAboutDuplicates(logger: Logger, duplicates: List<String>) {
    if (duplicates.isEmpty()) return

    logger.warn(
        "  ! ${duplicates.size} coordinate(s) are claimed by more than one library and the " +
            "last record wins: ${duplicates.joinToString()}."
    )
}

private fun render(library: LocalLibrary): String {
    val name = library.library.padEnd(NAME_WIDTH)
    val version = library.version.padEnd(VERSION_WIDTH)
    val published = age(library.publishedAtInstant()).padEnd(AGE_WIDTH)
    val modules = "${library.modules.size} module(s)"

    return "    $name $version $published $modules  ${library.repository}"
}

private fun age(instant: Instant?): String {
    if (instant == null) return "unknown"

    val elapsed = Duration.between(instant, Instant.now())
    if (elapsed.toMinutes() < 1) return "just now"
    if (elapsed.toHours() < 1) return "${elapsed.toMinutes()} min ago"
    if (elapsed.toDays() < 1) return "${elapsed.toHours()} h ago"

    return "${elapsed.toDays()} d ago"
}
