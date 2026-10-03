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

private const val COORDINATE_SEPARATOR: Char = ':'

private const val SEPARATORS_IN_COORDINATE: Int = 1

/**
 * A `group:name` module coordinate, without a version.
 *
 * Substitution matches on this pair and never on the group alone. A local publish records the
 * exact modules it installed, so a sibling artefact in the same group that was not published
 * locally is never touched.
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
     * Parsing helpers, shared between Kreate's artefacts.
     *
     * @since 3.4.0
     */
    @InternalKreateApi
    public companion object {
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

            val separators = trimmed.count { it == COORDINATE_SEPARATOR }
            val isWellFormed = separators == SEPARATORS_IN_COORDINATE && group.isNotEmpty() && name.isNotEmpty()
            if (!isWellFormed) return null

            return LocalModule(group, name)
        }
    }
}
