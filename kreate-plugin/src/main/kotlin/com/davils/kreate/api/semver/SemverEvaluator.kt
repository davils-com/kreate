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
package com.davils.kreate.api.semver

import com.davils.kreate.vcs.SemanticVersion
import com.davils.kreate.vcs.VersionBump

internal object SemverEvaluator {
    fun evaluate(
        before: String,
        after: String,
        baseline: String,
        baselineVersion: SemanticVersion,
        version: SemanticVersion,
        breakingAnnounced: Boolean?
    ): SemverResult {
        val changes = ApiChangeClassifier.classify(ApiDumpParser.parse(before), ApiDumpParser.parse(after))
        val bump = bumpOf(changes)
        val required = bump.apply(baselineVersion)
        val problems = mutableListOf<String>()
        if (bump != VersionBump.PATCH && version.core < required) {
            problems += "The API changed in a way that needs version $required or higher, but the version is $version."
        }
        if (bump == VersionBump.MAJOR && breakingAnnounced == false) {
            problems += "The API breaks, but no commit since $baseline is marked as breaking " +
                "with '!' or BREAKING CHANGE."
        }
        return SemverResult(baseline, baselineVersion, version, changes, bump, problems)
    }

    private fun bumpOf(changes: List<ApiChange>): VersionBump {
        if (changes.any { change -> change.kind == ApiChangeKind.BREAKING }) return VersionBump.MAJOR
        if (changes.isNotEmpty()) return VersionBump.MINOR
        return VersionBump.PATCH
    }
}
