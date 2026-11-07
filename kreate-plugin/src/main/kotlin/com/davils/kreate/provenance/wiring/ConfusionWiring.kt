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

import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.provenance.ProvenanceTaskNames
import com.davils.kreate.provenance.task.DependencyConfusion
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/confusion.json"

private const val HTTPS_PROXY_VARIABLE: String = "HTTPS_PROXY"

private const val NO_PROXY_VARIABLE: String = "NO_PROXY"

internal fun Project.registerDependencyConfusion(assurance: AssuranceExtension) {
    val confusion = assurance.confusion
    confusion.configurations.convention(provider { productionConfigurations() })
    val checkedProject = this

    tasks.register<DependencyConfusion>(ProvenanceTaskNames.CONFUSION) {
        val buildscript = confusion.includeBuildscript
        val names = confusion.configurations
        val merged = checkedProject.provider { checkedProject.provenanceGraphs(names.get(), buildscript.get()) }
        graphs.set(merged.flatMap { graphList -> graphList })
        markers.set(buildscript.flatMap { included -> checkedProject.pluginMarkers(included) })
        popularModules.set(confusion.popularModules)
        allow.set(confusion.allow)
        internalGroups.set(assurance.internalGroups)
        checkPublicRepository.set(confusion.checkPublicRepository)
        publicRepository.set(confusion.publicRepository)
        val publicLookup = confusion.checkPublicRepository
        reuseWindow.set(publicLookup.flatMap { enabled -> windowOf(checkedProject, assurance, enabled) })
        offline.set(checkedProject.gradle.startParameter.isOffline)
        httpsProxy.set(checkedProject.providers.environmentVariable(HTTPS_PROXY_VARIABLE))
        noProxy.set(checkedProject.providers.environmentVariable(NO_PROXY_VARIABLE))
        documentFile.set(checkedProject.layout.buildDirectory.file(DOCUMENT_FILE))
    }
}

private fun windowOf(project: Project, assurance: AssuranceExtension, enabled: Boolean): Provider<Long> {
    if (enabled) return project.reuseWindowOf(assurance)
    return project.provider { 0L }
}
