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

package com.davils.kreate.system

import org.gradle.api.GradleException

/**
 * Every platform identifier Kreate can produce or consume.
 *
 * The set is closed on purpose. A platform id is not free text: it is the directory name inside
 * the JAR, the suffix of a published artifact id, and the string a loader resolves at runtime from
 * `os.name` and `os.arch`. A typo like `linux-amd64` would produce an artifact that publishes
 * cleanly and that no consumer ever resolves - a defect that only surfaces as an
 * `UnsatisfiedLinkError` on someone else's machine.
 *
 * **That argument is the reason 3.0.0 changed the set.** It was `x64` and `arm64`, and the loader
 * consumers actually run - `com.davils.arc.platform.Platform.identifier`, by way of
 * `com.davils:sira-native` - resolves `x86_64` and `aarch64`. Kreate was making exactly the mistake
 * this table exists to prevent, against itself.
 *
 * The values mirror what [currentPlatformId] produces, and `KnownPlatformsTest` pins them as
 * literal strings. They are a wire format rather than an implementation detail: a published JAR and
 * a consumer built a year apart have to agree on them, so they are written out rather than derived.
 *
 * @since 2.2.0
 */
internal val KNOWN_PLATFORM_IDS: Set<String> = buildSet {
    for (os in listOf("windows", "linux", "macos")) {
        for (arch in listOf(X86_64_ID, AARCH64_ID)) {
            add("$os-$arch")
        }
    }
}

/**
 * Validates a platform identifier against [KNOWN_PLATFORM_IDS].
 *
 * @param platformId The identifier to validate.
 * @param context A short description of where the identifier came from, used in the message.
 * @return The identifier, unchanged.
 * @throws GradleException If the identifier is not a known platform.
 * @since 2.2.0
 */
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

/**
 * Converts a platform identifier into the suffix used in a Gradle task name.
 *
 * `linux-x86_64` becomes `LinuxX86_64`, so that [com.davils.kreate.KreateTasks.Jni.nativeJar] yields
 * `kreateJniNativeJarLinuxX86_64`. The underscore survives: it is part of the identifier, and a task
 * name that dropped it would no longer round-trip to the platform it names.
 *
 * @param platformId The platform identifier.
 * @return The task name suffix in upper camel case.
 * @since 2.2.0
 */
internal fun platformTaskSuffix(platformId: String): String = platformId
    .split('-')
    .joinToString(separator = "") { segment ->
        segment.replaceFirstChar { char -> char.uppercaseChar() }
    }
