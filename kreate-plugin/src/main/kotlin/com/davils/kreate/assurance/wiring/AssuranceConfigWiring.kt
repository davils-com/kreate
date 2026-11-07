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
package com.davils.kreate.assurance.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.assurance.AssuranceTaskNames
import com.davils.kreate.assurance.task.AssuranceConfigLint
import com.davils.kreate.assurance.task.AssuranceConfigShow
import com.davils.kreate.assurance.task.AssuranceExplain
import com.davils.kreate.assurance.task.CurrentDateSource
import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.trivy.TrivyExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

private const val CONFIG_FILE: String = "kreate/assurance/config.json"

private const val LINT_DOCUMENT: String = "kreate/assurance/documents/config.json"

private val URL_PROPERTIES: List<String> = listOf(
    "kreate.network.osvUrl",
    "kreate.network.depsDevUrl",
    "kreate.trivy.downloadBaseUrl",
    "kreate.trivy.dbRepository"
)

internal fun Project.registerAssuranceTools(extension: KreateExtension, rules: List<IgnoreRuleInput>) {
    val assurance = extension.assurance
    val defaults = objects.newInstance(AssuranceExtension::class.java)
    val trivyDefaults = objects.newInstance(TrivyExtension::class.java)
    val owner = this

    tasks.register<AssuranceExplain>(AssuranceTaskNames.EXPLAIN)

    tasks.register<AssuranceConfigShow>(AssuranceTaskNames.CONFIG) {
        values.set(owner.provider { configValues(assurance, extension.trivy) })
        this.defaults.set(owner.provider { configValues(defaults, trivyDefaults) })
        outputFile.set(owner.layout.buildDirectory.file(CONFIG_FILE))
    }

    tasks.register<AssuranceConfigLint>(AssuranceTaskNames.CONFIG_LINT) {
        values.set(owner.provider { configValues(assurance, extension.trivy) })
        urls.set(owner.urlsOf(assurance))
        ignoreRules.set(rules)
        today.set(owner.providers.of(CurrentDateSource::class.java) { })
        baselineFile.from(assurance.baseline.file)
        documentFile.set(owner.layout.buildDirectory.file(LINT_DOCUMENT))
    }
}

private fun Project.urlsOf(assurance: AssuranceExtension): Provider<Map<String, String>> {
    val properties = URL_PROPERTIES.associateWith { name -> providers.gradleProperty(name) }
    return provider {
        val declared = properties.mapNotNull { (name, value) -> value.orNull?.let { url -> name to url } }
        declared.toMap() + ("confusion.publicRepository" to assurance.confusion.publicRepository.get())
    }
}

internal fun configValues(assurance: AssuranceExtension, trivy: TrivyExtension): Map<String, String> {
    val entries = mapOf(
        "enabled" to assurance.enabled,
        "runOnCheck" to assurance.runOnCheck,
        "failOn" to assurance.failOn,
        "minSeverity" to assurance.minSeverity,
        "reportOnly" to assurance.reportOnly,
        "forbidIgnoreOf" to assurance.forbidIgnoreOf,
        "internalGroups" to assurance.internalGroups,
        "maxAdvisoryAge" to assurance.maxAdvisoryAge,
        "reuseVerdictFor" to assurance.reuseVerdictFor,
        "baseline.maxSeverity" to assurance.baseline.maxSeverity,
        "baseline.failOnStale" to assurance.baseline.failOnStale,
        "reports.formats" to assurance.reports.formats,
        "advisories.enabled" to assurance.advisories.enabled,
        "inspection.enabled" to assurance.inspection.enabled,
        "inspection.failOnRisk" to assurance.inspection.failOnRisk,
        "inspection.failScore" to assurance.inspection.failScore,
        "inspection.trustedHosts" to assurance.inspection.trustedHosts,
        "trust.enabled" to assurance.trust.enabled,
        "trust.freshPackageDays" to assurance.trust.freshPackageDays,
        "trust.youngPackageDays" to assurance.trust.youngPackageDays,
        "trust.freshReleaseHours" to assurance.trust.freshReleaseHours,
        "trust.minScorecard" to assurance.trust.minScorecard,
        "confusion.enabled" to assurance.confusion.enabled,
        "confusion.allow" to assurance.confusion.allow,
        "confusion.checkPublicRepository" to assurance.confusion.checkPublicRepository,
        "confusion.publicRepository" to assurance.confusion.publicRepository,
        "dependencyPolicy.enabled" to assurance.dependencyPolicy.enabled,
        "dependencyPolicy.denied" to assurance.dependencyPolicy.denied,
        "dependencyPolicy.allowed" to assurance.dependencyPolicy.allowed,
        "dependencyPolicy.allowedRepositoryHosts" to assurance.dependencyPolicy.allowedRepositoryHosts,
        "dependencyPolicy.forbidDynamicVersions" to assurance.dependencyPolicy.forbidDynamicVersions,
        "dependencyPolicy.forbidChangingVersions" to assurance.dependencyPolicy.forbidChangingVersions,
        "dependencyPolicy.requireLocking" to assurance.dependencyPolicy.requireLocking,
        "dependencyPolicy.requireVerification" to assurance.dependencyPolicy.requireVerification,
        "dependencyPolicy.checkOutdated" to assurance.dependencyPolicy.checkOutdated,
        "dependencyPolicy.maxMajorBehind" to assurance.dependencyPolicy.maxMajorBehind,
        "dependencyPolicy.maxLibyears" to assurance.dependencyPolicy.maxLibyears,
        "workspace.enabled" to assurance.workspace.enabled,
        "trivy.enabled" to trivy.enabled,
        "trivy.version" to trivy.version,
        "trivy.useInstalled" to trivy.useInstalled
    )
    return entries.mapValues { (_, value) -> textOf(value.orNull) }
}

private fun textOf(value: Any?): String {
    if (value is Collection<*>) return sortedText(value.map { item -> item.toString() })
    if (value is Map<*, *>) return sortedText(value.entries.map { (key, item) -> "$key=$item" })
    return value?.toString().orEmpty()
}

private fun sortedText(items: List<String>): String = items.sorted().joinToString(", ")
