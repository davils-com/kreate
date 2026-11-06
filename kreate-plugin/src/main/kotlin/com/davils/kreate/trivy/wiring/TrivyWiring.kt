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
import com.davils.kreate.trivy.TrivyDirectoryScanExtension
import com.davils.kreate.trivy.TrivyExtension
import com.davils.kreate.trivy.TrivyTaskNames
import com.davils.kreate.trivy.task.TrivyDirectoryScan
import com.davils.kreate.trivy.task.TrivyFilesystemScan
import com.davils.kreate.trivy.task.TrivyLicenseScan
import com.davils.kreate.trivy.task.TrivyMisconfigScan
import com.davils.kreate.trivy.task.TrivyProvision
import com.davils.kreate.trivy.task.TrivyScan
import com.davils.kreate.trivy.task.TrivyScanAggregate
import com.davils.kreate.trivy.task.TrivySecretScan
import com.davils.kreate.trivy.task.TrivyToolInput
import com.davils.kreate.trivy.task.TrivyVulnerabilityScan
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

private val SOURCE_SCAFFOLDING_TASKS: Set<String> = setOf(
    JniTaskNames.INITIALIZE,
    CInteropTaskNames.INITIALIZE
)

private const val DOCUMENT_DIRECTORY: String = "kreate/assurance/documents"

private const val CACHE_DIRECTORY: String = "caches/kreate/trivy"

private const val EXECUTABLE_PROPERTY: String = "kreate.trivy.executable"

private const val DOWNLOAD_BASE_URL_PROPERTY: String = "kreate.trivy.downloadBaseUrl"

private const val DEFAULT_DOWNLOAD_BASE_URL: String = "https://github.com/aquasecurity/trivy/releases/download"

private const val HTTPS_PROXY_VARIABLE: String = "HTTPS_PROXY"

private const val NO_PROXY_VARIABLE: String = "NO_PROXY"

internal fun Project.initializeTrivy(extension: KreateExtension) {
    val trivyExtension = extension.trivy
    if (!trivyExtension.enabled.get()) return

    val isGated = extension.assurance.enabled.get()

    tasks.withType<TrivyScan>().configureEach { project.configureTool(tool, trivyExtension) }
    tasks.register<TrivyProvision>(TrivyTaskNames.PROVISION) { project.configureTool(tool, trivyExtension) }

    val trivySecretExtension = trivyExtension.secrets
    val secretScan = tasks.register<TrivySecretScan>(TrivyTaskNames.SECRETS) {
        failOnFindings.set(trivySecretExtension.failOnFindings)
        gated.set(isGated)
        documentFile.set(layout.buildDirectory.file("$DOCUMENT_DIRECTORY/trivy-secrets.json"))
        projectDirectory.set(layout.projectDirectory)
        secretConfig.from(trivySecretExtension.secretConfig)
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
        failOnFindings.set(trivyLicenseExtension.failOnFindings)
        gated.set(isGated)
        documentFile.set(layout.buildDirectory.file("$DOCUMENT_DIRECTORY/trivy-licenses.json"))
        severity.set(trivyLicenseExtension.severity.names())
        ignoredLicenses.set(trivyLicenseExtension.ignoredLicenses)
        configurations.set(trivyLicenseExtension.configurations)
        failOnMissingLockFiles.set(trivyLicenseExtension.failOnMissingLockFiles)
        lockFiles.setFrom(trivyLicenseExtension.lockFiles)
    }

    val trivyVulnerabilityExtension = trivyExtension.vulnerability
    val vulnerabilityScan = tasks.register<TrivyVulnerabilityScan>(TrivyTaskNames.VULNERABILITIES) {
        severity.set(trivyVulnerabilityExtension.severity.names())
        failOnFindings.set(trivyVulnerabilityExtension.failOnFindings)
        gated.set(isGated)
        documentFile.set(layout.buildDirectory.file("$DOCUMENT_DIRECTORY/trivy-vulnerabilities.json"))
        failOnMissingLockFiles.set(trivyVulnerabilityExtension.failOnMissingLockFiles)
        ignoreUnfixed.set(trivyVulnerabilityExtension.ignoreUnfixed)
        lockFiles.setFrom(trivyVulnerabilityExtension.lockFiles)
        usesService(trivyDatabaseService())
    }

    val directoryScans = directoryScans(trivyExtension, isGated)

    tasks.register<TrivyScanAggregate>(TrivyTaskNames.SCAN) {
        dependsOn(secretScan, licenseScan, vulnerabilityScan, directoryScans)
    }
}

private fun Project.configureTool(tool: TrivyToolInput, trivy: TrivyExtension) {
    tool.version.set(trivy.version)
    tool.useInstalled.set(trivy.useInstalled)
    tool.executable.set(providers.gradleProperty(EXECUTABLE_PROPERTY).orElse(trivy.executable))
    tool.checksums.set(trivy.checksums)
    tool.downloadBaseUrl.set(providers.gradleProperty(DOWNLOAD_BASE_URL_PROPERTY).orElse(DEFAULT_DOWNLOAD_BASE_URL))
    tool.cacheRoot.set(gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
    tool.offline.set(gradle.startParameter.isOffline)
    tool.httpsProxy.set(providers.environmentVariable(HTTPS_PROXY_VARIABLE))
    tool.noProxy.set(providers.environmentVariable(NO_PROXY_VARIABLE))
}

private fun Project.directoryScans(
    trivy: TrivyExtension,
    isGated: Boolean
): List<TaskProvider<out TrivyDirectoryScan>> {
    val misconfig = trivy.misconfig
    val filesystem = trivy.filesystem
    val scans = mutableListOf<TaskProvider<out TrivyDirectoryScan>>()

    if (misconfig.enabled.get()) {
        scans += tasks.register<TrivyMisconfigScan>(TrivyTaskNames.MISCONFIGURATION) {
            configureDirectoryScan(misconfig, isGated, "trivy-misconfig.json")
        }
    }

    if (filesystem.enabled.get()) {
        scans += tasks.register<TrivyFilesystemScan>(TrivyTaskNames.FILESYSTEM) {
            configureDirectoryScan(filesystem, isGated, "trivy-filesystem.json")
            usesService(trivyDatabaseService())
        }
    }

    return scans
}

private fun TrivyDirectoryScan.configureDirectoryScan(
    scan: TrivyDirectoryScanExtension,
    isGated: Boolean,
    documentName: String
) {
    severity.set(scan.severity.names())
    failOnFindings.set(scan.failOnFindings)
    gated.set(isGated)
    skipDirectories.set(scan.skipDirectories)
    projectDirectory.set(project.layout.projectDirectory)
    documentFile.set(project.layout.buildDirectory.file("$DOCUMENT_DIRECTORY/$documentName"))
}

private fun <T : Enum<T>> Provider<List<T>>.names(): Provider<List<String>> =
    map { levels -> levels.map { it.name } }
