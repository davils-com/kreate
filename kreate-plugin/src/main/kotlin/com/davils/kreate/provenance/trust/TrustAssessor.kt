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
package com.davils.kreate.provenance.trust

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.provenance.depsdev.PackageHistory
import com.davils.kreate.provenance.provenanceFinding
import java.time.Duration
import java.time.Instant

internal class TrustAssessor(private val thresholds: TrustThresholds, private val now: Instant) {
    fun assess(evidence: TrustEvidence): List<Finding> {
        val history = evidence.history ?: return listOf(unavailable(evidence, "deps.dev does not know the module."))
        val published = history.publishedAt(evidence.module.version)
            ?: return ageOf(evidence, history) + unavailable(evidence, "deps.dev does not know when it was published.")
        return ageOf(evidence, history) + releaseOf(evidence, published) + scorecardOf(evidence)
    }

    private fun ageOf(evidence: TrustEvidence, history: PackageHistory): List<Finding> {
        val first = history.firstPublished ?: return emptyList()
        val days = Duration.between(first, now).toDays()
        val attributes = mapOf("firstPublished" to first.toString(), "ageDays" to days.toString())
        if (days < thresholds.freshPackageDays) {
            return listOf(finding("FRESH_PACKAGE", evidence, "First published $days day(s) ago", attributes))
        }
        if (days < thresholds.youngPackageDays) {
            return listOf(finding("YOUNG_PACKAGE", evidence, "First published $days day(s) ago", attributes))
        }
        return emptyList()
    }

    private fun releaseOf(evidence: TrustEvidence, published: Instant): List<Finding> {
        val hours = Duration.between(published, now).toHours()
        if (hours >= thresholds.freshReleaseHours) return emptyList()
        val attributes = mapOf("publishedAt" to published.toString(), "ageHours" to hours.toString())
        val title = "Version ${evidence.module.version} was published $hours hour(s) ago"
        return listOf(finding("FRESH_RELEASE", evidence, title, attributes))
    }

    private fun scorecardOf(evidence: TrustEvidence): List<Finding> {
        val score = evidence.scorecard ?: return emptyList()
        if (score >= thresholds.minScorecard) return emptyList()
        val project = evidence.project.orEmpty()
        val attributes = mapOf("scorecard" to score.toString(), "project" to project)
        return listOf(finding("LOW_SCORECARD", evidence, "$project scores $score of 10", attributes))
    }

    private fun unavailable(evidence: TrustEvidence, title: String): Finding {
        val attributes = mapOf(REPORT_ONLY_ATTRIBUTE to true.toString())
        return finding("TRUST_DATA_UNAVAILABLE", evidence, title, attributes)
    }

    private fun finding(rule: String, evidence: TrustEvidence, title: String, attributes: Map<String, String>) =
        provenanceFinding(rule, evidence.module, title, attributes)
}
