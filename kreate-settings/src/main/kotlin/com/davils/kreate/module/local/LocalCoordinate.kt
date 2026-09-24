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

import com.davils.kreate.InternalKreateApi
import java.io.Serializable

/**
 * The version suffix that marks a locally published build.
 *
 * Every local publication carries it, and the local Maven repository is declared on the consumer
 * side with `mavenContent { snapshotsOnly() }`. Together those two facts are what make the
 * feature safe: a locally published `3.0.0-SNAPSHOT` is a different coordinate from the released
 * `3.0.0`, and the repository holding it is never consulted for anything else.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"

/**
 * The version Gradle reports for a project that never had one assigned.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public const val UNSPECIFIED_VERSION: String = "unspecified"

/**
 * The separator between the group and the name of a module coordinate.
 *
 * @since 3.2.0
 */
private const val COORDINATE_SEPARATOR: Char = ':'

/**
 * A `group:name` module coordinate, without a version.
 *
 * Substitution matches on this pair and never on the group alone. A local publish records the
 * exact modules it installed, so a multiplatform library contributes `arc`, `arc-jvm`,
 * `arc-android` and `arc-wasm-js` as four separate entries and each substitutes on its own. A
 * sibling artefact in the same group that was not published locally is then never touched, which
 * is the property a group wildcard would lose.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public data class LocalModule(
    /**
     * The group of the module.
     * @since 3.2.0
     */
    val group: String,
    /**
     * The name of the module.
     * @since 3.2.0
     */
    val name: String
) : Serializable {
    /**
     * The coordinate in its `group:name` form.
     *
     * @since 3.2.0
     */
    val coordinate: String get() = "$group$COORDINATE_SEPARATOR$name"

    /**
     * Construction helpers, shared between Kreate's artefacts.
     *
     * @since 3.4.0
     */
    @InternalKreateApi
    public companion object {
        /**
         * The serial version identifier.
         *
         * `IsolatedAction` is `Serializable`, so everything the settings plugin hands to
         * `beforeProject` has to be too.
         *
         * @since 3.2.0
         */
        private const val serialVersionUID: Long = 1L

        /**
         * Parses a `group:name` coordinate.
         *
         * @param value The coordinate to parse.
         * @return The parsed module, or `null` if the value is not a `group:name` pair.
         * @since 3.2.0
         */
        public fun parse(value: String): LocalModule? {
            val trimmed = value.trim()
            val group = trimmed.substringBefore(COORDINATE_SEPARATOR).trim()
            val name = trimmed.substringAfter(COORDINATE_SEPARATOR, missingDelimiterValue = "").trim()

            val wellFormed = trimmed.count { it == COORDINATE_SEPARATOR } == 1 &&
                group.isNotEmpty() &&
                name.isNotEmpty()

            return if (wellFormed) LocalModule(group, name) else null
        }
    }
}

/**
 * Returns the version a local publication of [version] carries.
 *
 * Idempotent, so that a project whose version already ends in the suffix is left alone rather
 * than growing a second one.
 *
 * @param version The version the project resolved.
 * @return The version with the snapshot suffix applied.
 * @throws IllegalArgumentException If the version is blank or was never assigned, because
 * `unspecified-SNAPSHOT` is not a version anyone should be able to publish.
 * @since 3.2.0
 */
@InternalKreateApi
public fun snapshotVersionOf(version: String): String {
    val trimmed = version.trim()
    require(trimmed.isNotEmpty() && trimmed != UNSPECIFIED_VERSION) {
        "Cannot derive a local version from '$version'. Kreate resolves the version from the " +
            "configured environment variable or project property; set one of them before " +
            "publishing locally."
    }

    return if (trimmed.endsWith(SNAPSHOT_SUFFIX)) trimmed else "$trimmed$SNAPSHOT_SUFFIX"
}
