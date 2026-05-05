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

package com.davils.kreate.module.project.trivy

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.project.trivy.tasks.TrivyVulnerabilityScan
import com.davils.kreate.module.project.trivy.tasks.TrivyLicenseScan
import com.davils.kreate.module.project.trivy.tasks.TrivySecretScan
import com.davils.kreate.module.project.trivy.tasks.TrivyScan
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.named

/**
 * Initializes the Trivy module for the project.
 *
 * This function sets up dependency locking and registers the task for generating
 * Trivy-compatible lock files if the module is enabled.
 *
 * @param extension The Kreate extension containing module configuration.
 * @since 1.2.0
 */
internal fun Project.initializeTrivy(extension: KreateExtension) {
    val trivyExtension = extension.project.trivy
    if (!trivyExtension.enabled.get()) {
        return
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    val trivySecretExtension = trivyExtension.secrets
    val secretScan = tasks.register<TrivySecretScan>("trivySecretScan") {
        failOnFindings.set(trivySecretExtension.failOnFindings)
        secretConfig.set(trivySecretExtension.secretConfig)
        severity.set(trivySecretExtension.severity.map { it.map { s -> s.name } })
        sourceFiles.setFrom(trivySecretExtension.sourceFiles)
    }

    val trivyLicenseExtension = trivyExtension.license
    val licenseScan = tasks.register<TrivyLicenseScan>("trivyLicenseScan") {
        failOnForbidden.set(trivyLicenseExtension.failOnForbidden)
        severity.set(trivyLicenseExtension.severity.map { it.map { s -> s.name } })
        ignoredLicenses.set(trivyLicenseExtension.ignoredLicenses)
        fullLicenseScan.set(trivyLicenseExtension.fullLicenseScan)
        lockFiles.setFrom(trivyLicenseExtension.fullLicenseScan.map { full ->
            if (full) project.layout.projectDirectory else trivyLicenseExtension.lockFiles
        })
    }

    val trivyVulnerabilityExtension = trivyExtension.vulnerability
    val vulnerabilityScan = tasks.register<TrivyVulnerabilityScan>("trivyVulnerabilityScan") {
        severity.set(trivyVulnerabilityExtension.score.map { it.map { s -> s.name } })
        failOnFindings.set(trivyVulnerabilityExtension.failOnFindings)
        lockFiles.setFrom(trivyVulnerabilityExtension.lockFiles)
    }

    tasks.register<TrivyScan>("trivyScan") {
        dependsOn(secretScan, licenseScan, vulnerabilityScan)
    }
}
