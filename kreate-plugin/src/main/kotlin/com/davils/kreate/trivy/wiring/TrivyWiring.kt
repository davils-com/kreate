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

package com.davils.kreate.trivy.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.cinterop.CInteropTaskNames
import com.davils.kreate.jni.JniTaskNames
import com.davils.kreate.trivy.TrivyTaskNames
import com.davils.kreate.trivy.task.TrivyLicenseScan
import com.davils.kreate.trivy.task.TrivyScanAggregate
import com.davils.kreate.trivy.task.TrivySecretScan
import com.davils.kreate.trivy.task.TrivyVulnerabilityScan
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

private val SOURCE_SCAFFOLDING_TASKS: Set<String> = setOf(
    JniTaskNames.INITIALIZE,
    CInteropTaskNames.INITIALIZE
)

internal fun Project.initializeTrivy(extension: KreateExtension) {
    val trivyExtension = extension.trivy
    if (!trivyExtension.enabled.get()) return

    val trivySecretExtension = trivyExtension.secrets
    val secretScan = tasks.register<TrivySecretScan>(TrivyTaskNames.SECRETS) {
        failOnFindings.set(trivySecretExtension.failOnFindings)
        secretConfig.set(trivySecretExtension.secretConfig)
        severity.set(trivySecretExtension.severity.names())
        sourceFiles.setFrom(trivySecretExtension.sourceFiles)
        mustRunAfter(tasks.matching { it.name in SOURCE_SCAFFOLDING_TASKS })
    }
    if (trivySecretExtension.runOnCheck.get()) {
        tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
            dependsOn(secretScan)
        }
    }

    val trivyLicenseExtension = trivyExtension.license
    val productionConfigurations = provider { productionConfigurations() }
    trivyLicenseExtension.configurations.convention(productionConfigurations)
    val licenseScan = tasks.register<TrivyLicenseScan>(TrivyTaskNames.LICENSES) {
        failOnFindings.set(trivyLicenseExtension.failOnForbidden)
        severity.set(trivyLicenseExtension.severity.names())
        ignoredLicenses.set(trivyLicenseExtension.ignoredLicenses)
        configurations.set(trivyLicenseExtension.configurations)
        lockFiles.setFrom(trivyLicenseExtension.lockFiles)
    }

    val trivyVulnerabilityExtension = trivyExtension.vulnerability
    val vulnerabilityScan = tasks.register<TrivyVulnerabilityScan>(TrivyTaskNames.VULNERABILITIES) {
        severity.set(trivyVulnerabilityExtension.score.names())
        failOnFindings.set(trivyVulnerabilityExtension.failOnFindings)
        lockFiles.setFrom(trivyVulnerabilityExtension.lockFiles)
        usesService(trivyDatabaseService())
    }

    tasks.register<TrivyScanAggregate>(TrivyTaskNames.SCAN) {
        dependsOn(secretScan, licenseScan, vulnerabilityScan)
    }
}

private fun <T : Enum<T>> Provider<List<T>>.names(): Provider<List<String>> =
    map { levels -> levels.map { it.name } }
