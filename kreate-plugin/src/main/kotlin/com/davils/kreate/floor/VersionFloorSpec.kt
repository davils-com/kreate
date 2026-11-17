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

package com.davils.kreate.floor

/**
 * The lowest version a tool may resolve for the modules of one group.
 *
 * @property group The group whose modules are raised; it matches itself and every subgroup.
 * @property version The lowest version that may be resolved, such as `2.22.3`.
 * @since 5.0.0
 */
public class VersionFloorSpec internal constructor(
    public val group: String,
    public val version: String
) {
    private val excludedModules: MutableSet<String> = mutableSetOf()

    internal val excluded: Set<String>
        get() = excludedModules.toSet()

    /**
     * Keeps the requested version of the named modules of [group], such as an annotations artifact
     * that follows a numbering of its own.
     *
     * @param names The names of the modules, without their group.
     * @since 5.0.0
     */
    public fun exclude(vararg names: String) {
        excludedModules += names
    }
}
