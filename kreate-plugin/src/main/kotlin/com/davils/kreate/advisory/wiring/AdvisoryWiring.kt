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
package com.davils.kreate.advisory.wiring

import com.davils.kreate.advisory.AdvisoryTaskNames
import com.davils.kreate.advisory.task.AdvisoryAudit
import com.davils.kreate.advisory.task.ReuseWindowSource
import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.dependency.ResolvedGraph
import com.davils.kreate.dependency.wiring.resolvedGraphOf
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/osv.json"

private const val CACHE_DIRECTORY: String = "caches/kreate/osv"

private const val OSV_URL_PROPERTY: String = "kreate.network.osvUrl"

private const val DEFAULT_OSV_URL: String = "https://api.osv.dev"

private const val HTTPS_PROXY_VARIABLE: String = "HTTPS_PROXY"

private const val NO_PROXY_VARIABLE: String = "NO_PROXY"

internal fun Project.registerAdvisoryAudit(assurance: AssuranceExtension) {
    val advisories = assurance.advisories
    advisories.configurations.convention(provider { productionConfigurations() })

    tasks.register<AdvisoryAudit>(AdvisoryTaskNames.AUDIT) {
        val auditProject = project
        graphs.set(project.provider { auditProject.graphsOf(advisories.configurations.get()) }.flatMap(::merged))
        internalGroups.set(assurance.internalGroups)
        osvUrl.set(project.providers.gradleProperty(OSV_URL_PROPERTY).orElse(DEFAULT_OSV_URL))
        reuseWindow.set(auditProject.reuseWindowOf(assurance))
        offline.set(project.gradle.startParameter.isOffline)
        cacheDirectory.set(project.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
        httpsProxy.set(project.providers.environmentVariable(HTTPS_PROXY_VARIABLE))
        noProxy.set(project.providers.environmentVariable(NO_PROXY_VARIABLE))
        documentFile.set(project.layout.buildDirectory.file(DOCUMENT_FILE))
    }
}

private fun Project.reuseWindowOf(assurance: AssuranceExtension): Provider<Long> {
    val window = assurance.reuseVerdictFor.map { duration -> duration.seconds }
    return providers.of(ReuseWindowSource::class.java) { parameters.windowSeconds.set(window) }
}

private fun Project.graphsOf(names: Set<String>): List<Provider<ResolvedGraph>> {
    val resolvable = configurations.filter { configuration -> isAudited(configuration, names) }
    return resolvable.map { configuration ->
        configuration.incoming.resolutionResult.rootComponent.map(::resolvedGraphOf)
    }
}

private fun isAudited(configuration: Configuration, names: Set<String>): Boolean =
    configuration.name in names && configuration.isCanBeResolved

private fun Project.merged(graphs: List<Provider<ResolvedGraph>>): Provider<List<ResolvedGraph>> {
    val merged = objects.listProperty(ResolvedGraph::class.java)
    graphs.forEach { graph -> merged.add(graph) }
    return merged
}
