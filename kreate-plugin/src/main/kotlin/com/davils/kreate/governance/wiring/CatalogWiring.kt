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
package com.davils.kreate.governance.wiring

import com.davils.kreate.advisory.AdvisoryTaskNames
import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.governance.GovernanceTaskNames
import com.davils.kreate.governance.task.DependencyAdd
import com.davils.kreate.governance.task.DependencyFix
import com.davils.kreate.governance.task.TrustThresholdsInput
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

private const val CATALOG_FILE: String = "gradle/libs.versions.toml"

private const val OSV_DOCUMENT: String = "kreate/assurance/documents/osv.json"

private const val FIX_DIFF: String = "kreate/dependencies/catalog-fix.diff"

private const val CACHE_DIRECTORY: String = "caches/kreate"

private const val DEPS_DEV_URL_PROPERTY: String = "kreate.network.depsDevUrl"

private const val DEFAULT_DEPS_DEV_URL: String = "https://api.deps.dev"

private const val OSV_URL_PROPERTY: String = "kreate.network.osvUrl"

private const val DEFAULT_OSV_URL: String = "https://api.osv.dev"

internal fun Project.registerCatalogTasks(assurance: AssuranceExtension) {
    val policy = assurance.dependencyPolicy
    val trust = assurance.trust
    val confusion = assurance.confusion
    val catalog = isolated.rootProject.projectDirectory.file(CATALOG_FILE)
    val owner = this

    tasks.register<DependencyAdd>(GovernanceTaskNames.ADD) {
        catalogFile.set(catalog)
        internalGroups.set(assurance.internalGroups)
        denied.set(policy.denied)
        allowed.set(policy.allowed)
        popularModules.set(confusion.popularModules)
        typosquatAllow.set(confusion.allow)
        trustThresholds.set(
            owner.provider {
                TrustThresholdsInput(
                    trust.freshPackageDays.get(),
                    trust.youngPackageDays.get(),
                    trust.freshReleaseHours.get(),
                    trust.minScorecard.get()
                )
            }
        )
        osvUrl.set(owner.providers.gradleProperty(OSV_URL_PROPERTY).orElse(DEFAULT_OSV_URL))
        depsDevUrl.set(owner.providers.gradleProperty(DEPS_DEV_URL_PROPERTY).orElse(DEFAULT_DEPS_DEV_URL))
        offline.set(owner.gradle.startParameter.isOffline)
        cacheDirectory.set(owner.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
        httpsProxy.set(owner.providers.environmentVariable("HTTPS_PROXY"))
        noProxy.set(owner.providers.environmentVariable("NO_PROXY"))
    }

    val audit = tasks.names.contains(AdvisoryTaskNames.AUDIT)
    tasks.register<DependencyFix>(GovernanceTaskNames.FIX) {
        catalogFile.set(catalog)
        advisoryDocuments.from(owner.layout.buildDirectory.file(OSV_DOCUMENT))
        declared.set(policy.configurations.flatMap { names -> owner.declaredDependencies(names) })
        diffFile.set(owner.layout.buildDirectory.file(FIX_DIFF))
        if (audit) dependsOn(AdvisoryTaskNames.AUDIT)
    }
}
