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

import com.davils.kreate.floor.version.VersionFloorRule
import org.gradle.api.Action
import javax.inject.Inject

/**
 * Raises the dependencies of the tools a build runs to versions without known vulnerabilities.
 *
 * A floor applies to the classpaths of the tools Kreate wires up: Dokka, Detekt, Kover and the
 * compiler, plugin and BouncyCastle classpaths of the Kotlin Gradle plugin. It never touches the
 * compile or runtime classpath of the project and only ever raises a version, never lowers it. The
 * raised classpaths are locked and scanned like every other configuration.
 *
 * @since 4.3.0
 */
public abstract class ToolClasspathsExtension @Inject constructor() {
    private val floors: MutableList<VersionFloorSpec> = mutableListOf()

    /**
     * Raises every module of [group] that a tool resolves below [version] to [version].
     *
     * @param group The group whose modules are raised; it matches itself and every subgroup.
     * @param version The lowest version that may be resolved, such as `2.22.3`.
     * @throws IllegalArgumentException If [group] is blank or [version] is no numeric version.
     * @since 4.3.0
     */
    public fun floor(group: String, version: String) {
        floor(group, version) { }
    }

    /**
     * Raises every module of [group] that a tool resolves below [version] to [version], configured by
     * [action], for example to exclude a module.
     *
     * @param group The group whose modules are raised; it matches itself and every subgroup.
     * @param version The lowest version that may be resolved, such as `2.22.3`.
     * @param action The configuration action.
     * @throws IllegalArgumentException If [group] is blank or [version] is no numeric version.
     * @since 4.3.0
     */
    public fun floor(group: String, version: String, action: Action<VersionFloorSpec>) {
        require(group.isNotBlank()) { "A version floor needs a group." }
        require(VersionFloorRule.isNumeric(version)) { "'$version' is no numeric version such as 2.22.3." }
        val spec = VersionFloorSpec(group, version)
        action.execute(spec)
        floors += spec
    }

    internal fun declaredFloors(): List<VersionFloorSpec> = floors.toList()
}
