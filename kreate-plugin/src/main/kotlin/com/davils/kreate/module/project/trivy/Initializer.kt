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
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Initializes the Trivy module for the project.
 *
 * This function sets up dependency locking and registers the task for generating
 * Trivy-compatible lock files if the module is enabled.
 *
 * @param extension The Kreate extension containing module configuration.
 * @since 1.0.0
 */
internal fun Project.initializeTrivy(extension: KreateExtension) {
    val trivyExtension = extension.project.trivy
    if (!trivyExtension.enabled.get()) {
        return
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.register<TrivySecretScan>("trivyDetektSecrets") {
        failOnFindings.set(true)
        secretConfig.set(rootProject.file("trivy-secret.yaml"))
        severity.set("HIGH,CRITICAL,MEDIUM,LOW")
        sourceFiles.from(
            fileTree(projectDir) {
                include("src/**/*.kt", "src/**/*.java", "**/*.yaml", "**/*.yml", "**/*.env", "**/*.properties", "**/*.json")
            }
        )
    }

    val trivyLicenseExtension = trivyExtension.license
    tasks.register<TrivyLicenseScan>("trivyLicenseScan") {
        failOnForbidden.set(trivyLicenseExtension.failOnForbidden)
        severity.set(trivyLicenseExtension.severity.map { it.map { s -> s.name } })
        ignoredLicenses.set(trivyLicenseExtension.ignoredLicenses)
        fullLicenseScan.set(trivyLicenseExtension.fullLicenseScan)
        lockFiles.setFrom(trivyLicenseExtension.fullLicenseScan.map { full ->
            if (full) project.layout.projectDirectory else trivyLicenseExtension.lockFiles
        })
    }

    val trivyVulnerabilityExtension = trivyExtension.vulnerability
    tasks.register<TrivyVulnerabilityScan>("trivyVulnerabilityScan") {
        severity.set(trivyVulnerabilityExtension.score.map { it.map { s -> s.name } })
        failOnFindings.set(trivyVulnerabilityExtension.failOnFindings)
        lockFiles.from(trivyVulnerabilityExtension.lockFiles)
    }
}
