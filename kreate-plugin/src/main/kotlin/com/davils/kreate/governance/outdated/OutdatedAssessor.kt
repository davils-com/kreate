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
package com.davils.kreate.governance.outdated

import com.davils.kreate.advisory.version.MavenVersion
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.provenance.depsdev.PackageHistory
import java.time.Duration
import java.util.Locale

private const val DAYS_PER_YEAR: Double = 365.25

private const val LARGEST_SHOWN: Int = 5

private val PRE_RELEASE: Regex = Regex("(?i)[.-](alpha|beta|rc|cr|m\\d+|milestone|snapshot|dev|preview|eap|pre)")

private val LEADING_NUMBER: Regex = Regex("^(\\d{1,9})")

internal class OutdatedAssessor(private val settings: OutdatedSettings) {
    fun assess(entries: List<Pair<ModuleCoordinate, PackageHistory>>): OutdatedResult {
        val modules = entries.mapNotNull { (module, history) -> outdatedOf(module, history) }
        val total = modules.sumOf { module -> module.libyears ?: 0.0 }
        val findings = modules.mapNotNull { module -> majorFindingOf(module) } + libyearFindingOf(modules, total)
        return OutdatedResult(findings, modules, total)
    }

    private fun outdatedOf(module: ModuleCoordinate, history: PackageHistory): OutdatedModule? {
        val releases = history.published.keys.filter { version -> isRelease(version) }
        val latest = history.latest?.takeIf { version -> isRelease(version) }
            ?: releases.maxByOrNull { version -> MavenVersion(version) }
            ?: return null
        if (MavenVersion(latest) <= MavenVersion(module.version)) return null

        val current = majorOf(module.version) ?: return null
        val newest = majorOf(latest) ?: return null
        val majors = releases.mapNotNull { version -> majorOf(version) }.toSet()
        val behind = majors.count { major -> major in current + 1..newest }
        return OutdatedModule(module, latest, behind, libyearsOf(history, module.version, latest))
    }

    private fun libyearsOf(history: PackageHistory, installed: String, latest: String): Double? {
        val from = history.publishedAt(installed) ?: return null
        val to = history.publishedAt(latest) ?: return null
        val days = Duration.between(from, to).toDays()
        if (days <= 0) return null
        return days / DAYS_PER_YEAR
    }

    private fun majorFindingOf(module: OutdatedModule): Finding? {
        val maximum = settings.maxMajorBehind ?: return null
        if (module.majorsBehind <= maximum) return null
        val name = module.module.module
        val title = "$name is ${module.majorsBehind} major release(s) behind, latest ${module.latest}"
        val attributes = mapOf("behind" to module.majorsBehind.toString(), "latest" to module.latest)
        return ruleFinding("OUTDATED_MAJOR", module.module, title, attributes).copy(fixedVersion = module.latest)
    }

    private fun libyearFindingOf(modules: List<OutdatedModule>, total: Double): List<Finding> {
        val limit = settings.maxLibyears ?: return emptyList()
        if (total <= limit) return emptyList()
        val largest = modules.sortedByDescending { module -> module.libyears ?: 0.0 }.take(LARGEST_SHOWN)
        val names = largest.joinToString { module -> "${module.module.module} (${format(module.libyears ?: 0.0)})" }
        val title = "The direct dependencies are ${format(total)} libyears behind, more than ${format(limit)}"
        val attributes = mapOf("libyears" to format(total), "largest" to names)
        return listOf(ruleFinding("LIBYEAR_EXCEEDED", null, title, attributes))
    }

    private fun isRelease(version: String): Boolean = !PRE_RELEASE.containsMatchIn(version)

    private fun majorOf(version: String): Long? {
        val match = LEADING_NUMBER.find(version) ?: return null
        return match.groupValues[1].toLong()
    }

    private fun format(years: Double): String = String.format(Locale.ROOT, "%.1f", years)
}
