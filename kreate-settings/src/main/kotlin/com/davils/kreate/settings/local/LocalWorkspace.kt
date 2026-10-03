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
import java.io.Serializable

private const val SINGLE_CLAIM: Int = 1

/**
 * Everything that is currently published to the local Maven repository.
 *
 * Serializable because the settings plugin hands it to `GradleLifecycle.beforeProject`, whose
 * `IsolatedAction` may only capture data.
 *
 * @since 3.2.0
 */
@InternalKreateApi
public data class LocalWorkspace(
    /**
     * The recorded libraries, ordered by name.
     * @since 3.2.0
     */
    val libraries: List<LocalLibrary>
) : Serializable {
    /**
     * Whether nothing is published locally.
     *
     * @since 3.2.0
     */
    val isEmpty: Boolean get() = libraries.isEmpty()

    /**
     * The version to substitute, keyed by `group:name`.
     *
     * A coordinate published by two libraries at once is a mistake on the producer side; the later
     * entry wins and [duplicateCoordinates] names the overlap.
     *
     * @since 3.2.0
     */
    val substitutions: Map<String, String> by lazy {
        libraries.flatMap { library ->
            library.modules.map { module -> module.coordinate to library.version }
        }.toMap()
    }

    /**
     * Coordinates claimed by more than one library.
     *
     * @since 3.2.0
     */
    val duplicateCoordinates: List<String> by lazy {
        val coordinates = libraries.flatMap { library -> library.modules.map { module -> module.coordinate } }
        val claims = coordinates.groupingBy { it }.eachCount()
        val duplicates = claims.filterValues { count -> count > SINGLE_CLAIM }.keys
        duplicates.sorted()
    }

    /**
     * Construction helpers, shared between Kreate's artefacts.
     *
     * @since 3.4.0
     */
    @InternalKreateApi
    public companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * A workspace with nothing published.
         *
         * @since 3.2.0
         */
        public val EMPTY: LocalWorkspace = LocalWorkspace(emptyList())
    }
}

/**
 * Returns a copy of this workspace holding only the named libraries.
 *
 * @param names The library names to keep.
 * @return The narrowed workspace.
 * @since 3.2.0
 */
@InternalKreateApi
public fun LocalWorkspace.restrictedTo(names: Set<String>): LocalWorkspace =
    LocalWorkspace(libraries.filter { it.library in names })
