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

package com.davils.kreate.publish.guard

internal const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"

private const val IDENTIFIER: String = """(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)"""

private const val CORE: String = """(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"""

private val RELEASE_VERSION: Regex = Regex("""^$CORE(?:-($IDENTIFIER(?:\.$IDENTIFIER)*))?$""")

internal object ReleaseGuardRules {
    fun problemsOf(candidate: ReleaseCandidate): List<String> {
        val declared = declaredProblem(candidate)
        if (declared != null) return listOf(declared)
        val tag = candidate.tag ?: return listOfNotNull(snapshotProblem(candidate.resolvedVersion))
        return releaseProblems(candidate, tag)
    }

    fun isRelease(version: String): Boolean = RELEASE_VERSION.matches(version)

    private fun declaredProblem(candidate: ReleaseCandidate): String? {
        val declared = candidate.declaredVersion
            ?: return "The build declares no version, so the guard cannot tell what it may publish."
        val resolved = candidate.resolvedVersion.removeSuffix(SNAPSHOT_SUFFIX)
        if (declared == resolved) return null
        return "The build resolved the version '$resolved', but the build declares '$declared'. " +
            "Change the declared version, never the environment."
    }

    private fun snapshotProblem(version: String): String? {
        if (version.endsWith(SNAPSHOT_SUFFIX)) return null
        return "A build without a release tag must publish a snapshot, but it resolved '$version'."
    }

    private fun releaseProblems(candidate: ReleaseCandidate, tag: String): List<String> {
        val prefix = candidate.tagPrefix
        if (!tag.startsWith(prefix)) return listOf("The release tag '$tag' does not start with '$prefix'.")
        val tagged = tag.removePrefix(prefix)
        val version = candidate.resolvedVersion
        if (version.endsWith(SNAPSHOT_SUFFIX)) return listOf("A tag build cannot publish the snapshot '$version'.")
        val snapshots = candidate.tools.filterValues { tool -> tool.endsWith(SNAPSHOT_SUFFIX) }
        return listOfNotNull(
            "The release tag '$tag' does not match the version '$version' of the build.".takeIf { tagged != version },
            "The release version '$version' is not a Semantic Versioning 2.0.0 release.".takeIf { !isRelease(version) },
            toolProblem(snapshots)
        )
    }

    private fun toolProblem(snapshots: Map<String, String>): String? {
        if (snapshots.isEmpty()) return null
        val tools = snapshots.entries.joinToString { (name, version) -> "$name $version" }
        return "A release cannot be built with snapshot tooling: $tools."
    }
}
