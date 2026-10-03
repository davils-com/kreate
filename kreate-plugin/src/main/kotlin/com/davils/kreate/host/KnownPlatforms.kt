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

package com.davils.kreate.host

import org.gradle.api.GradleException

private const val PLATFORM_ID_SEPARATOR: Char = '-'

internal val KNOWN_PLATFORM_IDS: Set<String> = OperatingSystem.entries
    .flatMap { operatingSystem -> platformIdsOf(operatingSystem) }
    .toSet()

internal fun platformIdOf(operatingSystem: OperatingSystem, architecture: Architecture): String =
    "${operatingSystem.id}$PLATFORM_ID_SEPARATOR${architecture.id}"

internal fun requireKnownPlatform(platformId: String, context: String): String {
    if (platformId in KNOWN_PLATFORM_IDS) {
        return platformId
    }

    throw GradleException(
        """
            '$platformId' is not a platform Kreate knows ($context).

            A platform identifier is the directory name inside the JAR and the suffix of the
            published artifact id, and the generated loader derives it at runtime from the
            consumer's `os.name` and `os.arch`. An identifier Kreate does not produce would
            publish cleanly and never be found.

            Supported: ${KNOWN_PLATFORM_IDS.sorted().joinToString(", ")}
        """.trimIndent()
    )
}

internal fun platformTaskSuffix(platformId: String): String {
    val segments = platformId.split(PLATFORM_ID_SEPARATOR)
    return segments.joinToString(separator = "") { segment -> segment.capitalized() }
}

private fun String.capitalized(): String = replaceFirstChar { char -> char.uppercaseChar() }

private fun platformIdsOf(operatingSystem: OperatingSystem): List<String> =
    Architecture.entries.map { architecture -> platformIdOf(operatingSystem, architecture) }
