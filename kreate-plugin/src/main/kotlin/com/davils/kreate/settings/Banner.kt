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

package com.davils.kreate.settings

import com.davils.kreate.KreateTasks
import com.davils.kreate.module.local.LOCAL_PROPERTY
import com.davils.kreate.module.local.LocalLibrary
import com.davils.kreate.module.local.LocalMode
import com.davils.kreate.module.local.publishedAtInstant
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * The column width of a library name in the banner.
 *
 * @since 3.2.0
 */
private const val NAME_WIDTH: Int = 14

/**
 * The column width of a version in the banner.
 *
 * @since 3.2.0
 */
private const val VERSION_WIDTH: Int = 20

/**
 * Announces that this build resolves from the local Maven repository.
 *
 * Printed unconditionally and at lifecycle level, because a build that quietly resolves different
 * artefacts than its version catalog says is the one thing this feature could plausibly be blamed
 * for. Every confusing symptom it can produce — a test failing against code that is not in the
 * catalog, a compile error about a method nobody added — is explained by these four lines, and
 * only if they are visible.
 *
 * @param mode The active local mode.
 * @param repository The local Maven repository being resolved from.
 * @since 3.2.0
 */
internal fun Settings.announceLocalMode(mode: LocalMode.Active, repository: File) {
    val logger = Logging.getLogger(Settings::class.java)
    val libraries = mode.workspace.libraries

    logger.lifecycle("")
    logger.lifecycle(
        "Kreate local mode is ON — ${libraries.size} librar${if (libraries.size == 1) "y" else "ies"} " +
            "resolved from $repository:"
    )
    libraries.forEach { library -> logger.lifecycle(render(library)) }

    val duplicates = mode.workspace.duplicateCoordinates
    if (duplicates.isNotEmpty()) {
        logger.warn(
            "  ! ${duplicates.size} coordinate(s) are claimed by more than one library and the " +
                "last record wins: ${duplicates.joinToString()}."
        )
    }

    logger.lifecycle("  Dependency locking is off. No lock file is read or written.")
    logger.lifecycle(
        "  Switch off with -P$LOCAL_PROPERTY=false; clear with ./gradlew ${KreateTasks.Local.CLEAN}."
    )
    logger.lifecycle("")
}

/**
 * Renders one library's line of the banner.
 *
 * @param library The recorded library.
 * @return The rendered line.
 * @since 3.2.0
 */
private fun render(library: LocalLibrary): String {
    val name = library.library.padEnd(NAME_WIDTH)
    val version = library.version.padEnd(VERSION_WIDTH)
    val modules = "${library.modules.size} module(s)"

    return "    $name $version ${age(library.publishedAtInstant()).padEnd(NAME_WIDTH)} $modules  ${library.repository}"
}

/**
 * Renders how long ago a publication happened.
 *
 * The age is in the banner because the most common confusion this feature produces is not "why is
 * it substituting" but "why is it substituting something I published an hour ago and have since
 * changed".
 *
 * @param instant When it happened, or `null` if the record has no usable timestamp.
 * @return A short human readable age.
 * @since 3.2.0
 */
private fun age(instant: Instant?): String {
    if (instant == null) return "unknown"

    val elapsed = Duration.between(instant, Instant.now())
    return when {
        elapsed.toMinutes() < 1 -> "just now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()} min ago"
        elapsed.toDays() < 1 -> "${elapsed.toHours()} h ago"
        else -> "${elapsed.toDays()} d ago"
    }
}
