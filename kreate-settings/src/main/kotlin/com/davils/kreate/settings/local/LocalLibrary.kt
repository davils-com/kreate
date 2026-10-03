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
import java.io.File
import java.io.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

private val logger: Logger = Logging.getLogger(LocalLibrary::class.java)

/**
 * A single locally published library.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public data class LocalLibrary(
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
     * Text rather than an [Instant], because this type crosses the configuration cache and Gradle
     * cannot serialize `java.time` types there on every supported version. Parse it with
     * [publishedAtInstant] where it is displayed.
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
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Parses the recorded publication time.
 *
 * @return The instant, or `null` if the record carries none or one that cannot be read.
 * @since 3.2.0
 */
@InternalKreateApi
public fun LocalLibrary.publishedAtInstant(): Instant? {
    val recorded = publishedAt ?: return null

    return try {
        Instant.parse(recorded)
    } catch (exception: DateTimeParseException) {
        logger.debug("Kreate cannot read the publication time '$recorded' of '$library'.", exception)
        null
    }
}
