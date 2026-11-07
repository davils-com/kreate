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

internal enum class VersionBump {
    PATCH,
    MINOR,
    MAJOR;

    fun apply(current: SemanticVersion): SemanticVersion = when (this) {
        MAJOR -> majorOf(current)
        MINOR -> minorOf(current)
        PATCH -> patchOf(current)
    }

    private fun majorOf(current: SemanticVersion): SemanticVersion {
        if (!current.isStable) return SemanticVersion(0, current.minor + 1, 0)
        return SemanticVersion(current.major + 1, 0, 0)
    }

    private fun minorOf(current: SemanticVersion): SemanticVersion {
        if (!current.isStable) return patchOf(current)
        return SemanticVersion(current.major, current.minor + 1, 0)
    }

    private fun patchOf(current: SemanticVersion): SemanticVersion {
        if (current.preRelease != null) return current.core
        return SemanticVersion(current.major, current.minor, current.patch + 1)
    }
}
