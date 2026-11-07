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
package com.davils.kreate.provenance.wiring

import com.davils.kreate.advisory.task.ReuseWindowSource
import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.provenance.ProvenanceTaskNames
import com.davils.kreate.provenance.task.DependencyTrust
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/trust.json"

private const val CACHE_DIRECTORY: String = "caches/kreate/deps-dev"

private const val DEPS_DEV_URL_PROPERTY: String = "kreate.network.depsDevUrl"

private const val DEFAULT_DEPS_DEV_URL: String = "https://api.deps.dev"

private const val HTTPS_PROXY_VARIABLE: String = "HTTPS_PROXY"

private const val NO_PROXY_VARIABLE: String = "NO_PROXY"

internal fun Project.registerDependencyTrust(assurance: AssuranceExtension) {
    val trust = assurance.trust
    trust.configurations.convention(provider { productionConfigurations() })
    val trustProject = this

    tasks.register<DependencyTrust>(ProvenanceTaskNames.TRUST) {
        val names = trust.configurations
        val included = trust.includeBuildscript
        val merged = trustProject.provider { trustProject.provenanceGraphs(names.get(), included.get()) }
        graphs.set(merged.flatMap { graphList -> graphList })
        internalGroups.set(assurance.internalGroups)
        depsDevUrl.set(trustProject.providers.gradleProperty(DEPS_DEV_URL_PROPERTY).orElse(DEFAULT_DEPS_DEV_URL))
        freshPackageDays.set(trust.freshPackageDays)
        youngPackageDays.set(trust.youngPackageDays)
        freshReleaseHours.set(trust.freshReleaseHours)
        minScorecard.set(trust.minScorecard)
        reuseWindow.set(trustProject.reuseWindowOf(assurance))
        offline.set(trustProject.gradle.startParameter.isOffline)
        cacheDirectory.set(trustProject.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
        httpsProxy.set(trustProject.providers.environmentVariable(HTTPS_PROXY_VARIABLE))
        noProxy.set(trustProject.providers.environmentVariable(NO_PROXY_VARIABLE))
        documentFile.set(trustProject.layout.buildDirectory.file(DOCUMENT_FILE))
    }
}

internal fun Project.reuseWindowOf(assurance: AssuranceExtension): Provider<Long> {
    val window = assurance.reuseVerdictFor.map { duration -> duration.seconds }
    return providers.of(ReuseWindowSource::class.java) { parameters.windowSeconds.set(window) }
}
