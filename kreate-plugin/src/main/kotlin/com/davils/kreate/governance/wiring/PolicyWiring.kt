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

import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.governance.GovernanceTaskNames
import com.davils.kreate.governance.task.DependencyOutdated
import com.davils.kreate.governance.task.DependencyPolicyCheck
import com.davils.kreate.provenance.wiring.provenanceGraphs
import com.davils.kreate.provenance.wiring.reuseWindowOf
import com.davils.kreate.trivy.wiring.productionConfigurations
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.kotlin.dsl.register

private const val POLICY_DOCUMENT: String = "kreate/assurance/documents/dependencies.json"

private const val OUTDATED_DOCUMENT: String = "kreate/assurance/documents/outdated.json"

private const val LOCK_FILE: String = "gradle.lockfile"

private const val VERIFICATION_FILE: String = "gradle/verification-metadata.xml"

private const val WRAPPER_FILE: String = "gradle/wrapper/gradle-wrapper.properties"

private const val CACHE_DIRECTORY: String = "caches/kreate/deps-dev"

private const val DEPS_DEV_URL_PROPERTY: String = "kreate.network.depsDevUrl"

private const val DEFAULT_DEPS_DEV_URL: String = "https://api.deps.dev"

internal fun Project.registerDependencyPolicy(assurance: AssuranceExtension) {
    val policy = assurance.dependencyPolicy
    policy.configurations.convention(provider { productionConfigurations() })
    val checked = this
    val root = isolated.rootProject.projectDirectory

    tasks.register<DependencyPolicyCheck>(GovernanceTaskNames.POLICY) {
        val names = policy.configurations
        val merged = checked.provider { checked.provenanceGraphs(names.get(), false) }
        graphs.set(merged.flatMap { graphList -> graphList })
        declared.set(names.flatMap { configurationNames -> checked.declaredDependencies(configurationNames) })
        forced.set(names.flatMap { configurationNames -> checked.forcedModules(configurationNames) })
        repositories.set(checked.repositoryDeclarations())
        denied.set(policy.denied)
        allowed.set(policy.allowed)
        allowedRepositoryHosts.set(policy.allowedRepositoryHosts)
        forbidDynamicVersions.set(policy.forbidDynamicVersions)
        forbidChangingVersions.set(policy.forbidChangingVersions)
        testLibraries.set(policy.testLibraries)
        requireLocking.set(policy.requireLocking)
        requireVerification.set(policy.requireVerification)
        lockMode.set(checked.dependencyLocking.lockMode.map { mode -> mode.name }.orElse(LockMode.DEFAULT.name))
        verificationMode.set(checked.gradle.startParameter.dependencyVerificationMode.name)
        lockFile.from(checked.dependencyLocking.lockFile.orElse(checked.layout.projectDirectory.file(LOCK_FILE)))
        verificationMetadata.from(root.file(VERIFICATION_FILE))
        wrapperProperties.from(root.file(WRAPPER_FILE))
        documentFile.set(checked.layout.buildDirectory.file(POLICY_DOCUMENT))
    }
}

internal fun Project.registerDependencyOutdated(assurance: AssuranceExtension) {
    val policy = assurance.dependencyPolicy
    val checked = this

    tasks.register<DependencyOutdated>(GovernanceTaskNames.OUTDATED) {
        declared.set(policy.configurations.flatMap { names -> checked.declaredDependencies(names) })
        internalGroups.set(assurance.internalGroups)
        maxMajorBehind.set(policy.maxMajorBehind)
        maxLibyears.set(policy.maxLibyears)
        depsDevUrl.set(checked.providers.gradleProperty(DEPS_DEV_URL_PROPERTY).orElse(DEFAULT_DEPS_DEV_URL))
        reuseWindow.set(checked.reuseWindowOf(assurance))
        offline.set(checked.gradle.startParameter.isOffline)
        cacheDirectory.set(checked.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
        httpsProxy.set(checked.providers.environmentVariable("HTTPS_PROXY"))
        noProxy.set(checked.providers.environmentVariable("NO_PROXY"))
        documentFile.set(checked.layout.buildDirectory.file(OUTDATED_DOCUMENT))
    }
}
