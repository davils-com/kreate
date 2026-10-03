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

/**
 * The version suffix that marks a locally published build.
 *
 * Every local publication carries it, and the consumer declares the local Maven repository with
 * `mavenContent { snapshotsOnly() }`, so a local `3.0.0-SNAPSHOT` can never shadow the released
 * `3.0.0`.
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
 * Returns the version a local publication of [version] carries.
 *
 * Idempotent, so that a version already ending in the suffix does not grow a second one.
 *
 * @param version The version the project resolved.
 * @return The version with the snapshot suffix applied.
 * @throws IllegalArgumentException If the version is blank or was never assigned.
 * @since 3.2.0
 */
@InternalKreateApi
public fun snapshotVersionOf(version: String): String {
    val trimmed = version.trim()
    val isAssigned = trimmed.isNotEmpty() && trimmed != UNSPECIFIED_VERSION
    require(isAssigned) {
        "Cannot derive a local version from '$version'. Kreate resolves the version from the " +
            "configured environment variable or project property; set one of them before " +
            "publishing locally."
    }

    if (trimmed.endsWith(SNAPSHOT_SUFFIX)) return trimmed

    return "$trimmed$SNAPSHOT_SUFFIX"
}
