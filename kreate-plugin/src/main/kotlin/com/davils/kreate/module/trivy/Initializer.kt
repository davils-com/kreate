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

package com.davils.kreate.module.trivy

import com.davils.kreate.KreateExtension
import com.davils.kreate.KreateTasks
import com.davils.kreate.module.trivy.tasks.TrivyLicenseScan
import com.davils.kreate.module.trivy.tasks.TrivyScan
import com.davils.kreate.module.trivy.tasks.TrivySecretScan
import com.davils.kreate.module.trivy.tasks.TrivyVulnerabilityScan
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * The tasks that write scaffolding into the source tree the secret scan reads.
 *
 * Gradle refuses a task whose input overlaps another task's output unless the two are ordered, and
 * with the secret scan on `check` the two meet in any build that initialises a native project. The
 * scan runs after them, which is also the order that scans what they wrote.
 */
private val SOURCE_SCAFFOLDING_TASKS: Set<String> = setOf(
    KreateTasks.Jni.INITIALIZE,
    KreateTasks.CInterop.INITIALIZE
)

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
    val trivyExtension = extension.trivy
    if (!trivyExtension.enabled.get()) {
        return
    }

    val trivySecretExtension = trivyExtension.secrets
    val secretScan = tasks.register<TrivySecretScan>(KreateTasks.Trivy.SECRETS) {
        failOnFindings.set(trivySecretExtension.failOnFindings)
        secretConfig.set(trivySecretExtension.secretConfig)
        severity.set(trivySecretExtension.severity.map { it.map { s -> s.name } })
        sourceFiles.setFrom(trivySecretExtension.sourceFiles)
        mustRunAfter(tasks.matching { it.name in SOURCE_SCAFFOLDING_TASKS })
    }
    if (trivySecretExtension.runOnCheck.get()) {
        tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
            dependsOn(secretScan)
        }
    }

    val trivyLicenseExtension = trivyExtension.license
    val licenseScan = tasks.register<TrivyLicenseScan>(KreateTasks.Trivy.LICENSES) {
        failOnForbidden.set(trivyLicenseExtension.failOnForbidden)
        severity.set(trivyLicenseExtension.severity.map { it.map { s -> s.name } })
        ignoredLicenses.set(trivyLicenseExtension.ignoredLicenses)
        lockFiles.setFrom(trivyLicenseExtension.lockFiles)
    }

    val trivyVulnerabilityExtension = trivyExtension.vulnerability
    val vulnerabilityScan = tasks.register<TrivyVulnerabilityScan>(KreateTasks.Trivy.VULNERABILITIES) {
        severity.set(trivyVulnerabilityExtension.score.map { it.map { s -> s.name } })
        failOnFindings.set(trivyVulnerabilityExtension.failOnFindings)
        lockFiles.setFrom(trivyVulnerabilityExtension.lockFiles)
    }

    tasks.register<TrivyScan>(KreateTasks.Trivy.SCAN) {
        dependsOn(secretScan, licenseScan, vulnerabilityScan)
    }
}
