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
package com.davils.kreate.vcs

import com.davils.kore.core.version.SemanticVersion
import com.davils.kore.core.version.nextPatch

internal enum class VersionBump {
    PATCH,
    MINOR,
    MAJOR;

    fun apply(current: SemanticVersion): SemanticVersion = when (this) {
        MAJOR -> majorOf(current)
        MINOR -> minorOf(current)
        PATCH -> current.nextPatch()
    }

    private fun majorOf(current: SemanticVersion): SemanticVersion {
        if (current.major == 0) return SemanticVersion(major = 0, minor = current.minor + 1)
        return SemanticVersion(major = current.major + 1)
    }

    private fun minorOf(current: SemanticVersion): SemanticVersion {
        if (current.major == 0) return current.nextPatch()
        return SemanticVersion(major = current.major, minor = current.minor + 1)
    }
}
