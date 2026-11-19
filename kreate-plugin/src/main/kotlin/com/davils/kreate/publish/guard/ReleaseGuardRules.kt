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

import com.davils.kore.core.text.SafeText
import com.davils.kore.core.version.SemanticVersion

internal const val SNAPSHOT_SUFFIX: String = "-SNAPSHOT"

internal object ReleaseGuardRules {
    fun problemsOf(candidate: ReleaseCandidate): List<String> {
        val declared = declaredProblem(candidate)
        if (declared != null) return listOf(declared)
        val tag = candidate.tag ?: return listOfNotNull(snapshotProblem(candidate.resolvedVersion))
        return releaseProblems(candidate, tag)
    }

    fun isRelease(version: String): Boolean {
        val parsed = SemanticVersion.parseOrNull(version) ?: return false
        return parsed.build.isEmpty()
    }

    private fun declaredProblem(candidate: ReleaseCandidate): String? {
        val declared = candidate.declaredVersion
            ?: return "The build declares no version, so the guard cannot tell what it may publish."
        val resolved = candidate.resolvedVersion.removeSuffix(SNAPSHOT_SUFFIX)
        if (declared == resolved) return null
        return "The build resolved the version ${SafeText.quoted(resolved)}, but the build declares " +
            "${SafeText.quoted(declared)}. " +
            "Change the declared version, never the environment."
    }

    private fun snapshotProblem(version: String): String? {
        if (version.endsWith(SNAPSHOT_SUFFIX)) return null
        return "A build without a release tag must publish a snapshot, but it resolved ${SafeText.quoted(version)}."
    }

    private fun releaseProblems(candidate: ReleaseCandidate, tag: String): List<String> {
        val prefix = candidate.tagPrefix
        val shownTag = SafeText.quoted(tag)
        if (!tag.startsWith(prefix)) {
            return listOf("The release tag $shownTag does not start with ${SafeText.quoted(prefix)}.")
        }
        val tagged = tag.removePrefix(prefix)
        val version = candidate.resolvedVersion
        val shownVersion = SafeText.quoted(version)
        if (version.endsWith(SNAPSHOT_SUFFIX)) return listOf("A tag build cannot publish the snapshot $shownVersion.")
        val snapshots = candidate.tools.filterValues { tool -> tool.endsWith(SNAPSHOT_SUFFIX) }
        val mismatch = "The release tag $shownTag does not match the version $shownVersion of the build."
        val unreleased = "The release version $shownVersion is not a Semantic Versioning 2.0.0 release."
        return listOfNotNull(
            mismatch.takeIf { tagged != version },
            unreleased.takeIf { !isRelease(version) },
            toolProblem(snapshots)
        )
    }

    private fun toolProblem(snapshots: Map<String, String>): String? {
        if (snapshots.isEmpty()) return null
        val tools = snapshots.entries.joinToString { (name, version) -> "$name $version" }
        return "A release cannot be built with snapshot tooling: $tools."
    }
}
