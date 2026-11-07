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
package com.davils.kreate.provenance

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.rule.ruleFinding
import com.davils.kreate.dependency.ModuleCoordinate
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.provenance.confusion.MavenMetadataClient
import com.davils.kreate.provenance.confusion.PublicRepositoryUnavailableException
import com.davils.kreate.provenance.marker.PluginMarker
import com.davils.kreate.provenance.marker.PluginMarkerChecker
import com.davils.kreate.provenance.typosquat.InflatedVersions
import com.davils.kreate.provenance.typosquat.TyposquatDetector
import java.time.Clock

private const val PARALLEL_REQUESTS: Int = 8

internal class ConfusionAuditor(
    private val typosquat: TyposquatDetector,
    private val markers: PluginMarkerChecker,
    private val publicRepository: (() -> MavenMetadataClient)?,
    private val internalGroups: Set<String>,
    private val clock: Clock
) {
    fun audit(graphs: List<ResolvedGraph>, pluginMarkers: List<PluginMarker>, offline: Boolean): FindingDocument {
        val modules = graphs.flatMap { graph -> graph.modules }.distinct()
        val unresolved = graphs.flatMap { graph -> graph.unresolved }.distinct()
        val local = typosquat.findingsOf(modules) + inflatedOf(modules) + markers.findingsOf(pluginMarkers)
        val lookup = publicRepository ?: return documentOf(local, unresolvedReason(unresolved))
        if (offline) return documentOf(local, "The public repository cannot be read in offline mode.")

        val internal = modules.filter { module -> isInternal(module, internalGroups) }
        return try {
            val published = publishedOf(lookup(), internal)
            documentOf(local + published, unresolvedReason(unresolved))
        } catch (cause: PublicRepositoryUnavailableException) {
            documentOf(local, cause.message.orEmpty())
        }
    }

    private fun inflatedOf(modules: List<ModuleCoordinate>): List<Finding> {
        val inflated = modules.filter { module -> InflatedVersions.isInflated(module.version) }
        return inflated.map { module ->
            val title = "Version ${module.version} of ${module.module} is unusually high"
            ruleFinding("SUSPICIOUS_VERSION", module, title)
        }
    }

    private fun publishedOf(client: MavenMetadataClient, modules: List<ModuleCoordinate>): List<Finding> {
        val distinct = modules.distinctBy { module -> module.module }
        val latest = boundedMap(distinct, PARALLEL_REQUESTS) { module ->
            client.latestVersionOf(module.group, module.name)
        }
        val published = distinct.zip(latest).filter { (_, version) -> version != null }
        return published.map { (module, version) -> confusionOf(module, version.orEmpty()) }
    }

    private fun confusionOf(module: ModuleCoordinate, publicVersion: String): Finding {
        val title = "${module.module} is internal, but the public repository publishes it up to $publicVersion"
        return ruleFinding("DEPENDENCY_CONFUSION", module, title, mapOf("publicVersion" to publicVersion))
    }

    private fun unresolvedReason(unresolved: List<String>): String? {
        if (unresolved.isEmpty()) return null
        return "These dependencies could not be resolved, so they were not checked: $unresolved"
    }

    private fun documentOf(findings: List<Finding>, reason: String?): FindingDocument {
        val now = clock.instant()
        val status = reason?.let { DocumentStatus.INCOMPLETE } ?: DocumentStatus.COMPLETE
        return FindingDocument(FindingSource.CONFUSION, status, now, findings, reason, now)
    }
}
