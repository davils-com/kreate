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

private val SEMANTIC: Regex =
    Regex("""^(\d{1,9})\.(\d{1,9})\.(\d{1,9})(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

private const val MAJOR_GROUP: Int = 1

private const val MINOR_GROUP: Int = 2

private const val PATCH_GROUP: Int = 3

private const val PRE_RELEASE_GROUP: Int = 4

internal data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null
) : Comparable<SemanticVersion> {
    val isStable: Boolean
        get() = major > 0

    val core: SemanticVersion
        get() = SemanticVersion(major, minor, patch)

    override fun compareTo(other: SemanticVersion): Int {
        val core = compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)
        if (core != 0) return core
        return comparePreReleases(preRelease, other.preRelease)
    }

    override fun toString(): String {
        val core = "$major.$minor.$patch"
        val suffix = preRelease?.let { label -> "-$label" }.orEmpty()
        return core + suffix
    }

    companion object {
        fun parse(text: String): SemanticVersion? {
            val match = SEMANTIC.matchEntire(text.trim()) ?: return null
            val values = match.groupValues
            val preRelease = values[PRE_RELEASE_GROUP].ifEmpty { null }
            return SemanticVersion(
                values[MAJOR_GROUP].toInt(),
                values[MINOR_GROUP].toInt(),
                values[PATCH_GROUP].toInt(),
                preRelease
            )
        }

        private fun comparePreReleases(first: String?, second: String?): Int {
            if (first == second) return 0
            if (first == null) return 1
            if (second == null) return -1
            val firstParts = first.split('.')
            val secondParts = second.split('.')
            val differing = firstParts.zip(secondParts).firstOrNull { (one, other) -> one != other }
                ?: return firstParts.size.compareTo(secondParts.size)
            return compareIdentifiers(differing.first, differing.second)
        }

        private fun compareIdentifiers(first: String, second: String): Int {
            val firstNumber = first.toIntOrNull()
            val secondNumber = second.toIntOrNull()
            if (firstNumber != null && secondNumber != null) return firstNumber.compareTo(secondNumber)
            if (firstNumber != null) return -1
            if (secondNumber != null) return 1
            return first.compareTo(second)
        }
    }
}
