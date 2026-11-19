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
package com.davils.kreate.standard.wiring

import com.davils.kreate.advisory.task.ReuseWindowSource
import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.assurance.AssuranceStandardsExtension
import com.davils.kreate.assurance.task.CurrentDateSource
import com.davils.kreate.gradle.GENERATED_PATHS
import com.davils.kreate.standard.StandardsTaskNames
import com.davils.kreate.standard.model.DeclarationValidator
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.provider.ServiceEndpoints
import com.davils.kreate.standard.task.StandardsAudit
import com.davils.kreate.standard.task.StandardsInventory
import com.davils.kreate.standard.task.StandardsReview
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

private const val INVENTORY_DOCUMENT: String = "kreate/assurance/documents/standards-inventory.json"

private const val AUDIT_DOCUMENT: String = "kreate/assurance/documents/standards.json"

private const val SNAPSHOTS_FILE: String = "kreate/standards/snapshots.json"

private const val CACHE_DIRECTORY: String = "caches/kreate/standards"

private const val NETWORK_PROPERTY_PREFIX: String = "kreate.network."

private const val HTTPS_PROXY_VARIABLE: String = "HTTPS_PROXY"

private const val NO_PROXY_VARIABLE: String = "NO_PROXY"

private const val GITHUB_TOKEN_VARIABLE: String = "GITHUB_TOKEN"

private const val MAX_PARALLEL_CACHE_USAGES: Int = 1

internal fun Project.registerStandards(assurance: AssuranceExtension) {
    val standards = assurance.standards
    val declarations = validatedDeclarations(standards)
    registerInventory(standards, declarations)
    val audit = registerAudit(assurance, declarations)
    registerReview(standards, declarations, audit)
}

private fun Project.registerAudit(
    assurance: AssuranceExtension,
    declarations: List<StandardDeclaration>
): TaskProvider<StandardsAudit> {
    val owner = this
    val standards = assurance.standards
    val cache = standardsCacheService()
    return tasks.register<StandardsAudit>(StandardsTaskNames.AUDIT) {
        usesService(cache)
        this.declarations.set(declarations)
        lockFile.from(standards.lockFile)
        lockPath.set(standards.lockFile.map { file -> lockPathOf(owner, file.asFile) })
        ServiceEndpoints.DEFAULTS.forEach { (service, url) ->
            endpoints.put(service, owner.providers.gradleProperty(NETWORK_PROPERTY_PREFIX + service).orElse(url))
        }
        reuseWindow.set(owner.reuseWindowOf(assurance))
        offline.set(owner.gradle.startParameter.isOffline)
        cacheDirectory.set(owner.gradle.gradleUserHomeDir.resolve(CACHE_DIRECTORY))
        httpsProxy.set(owner.providers.environmentVariable(HTTPS_PROXY_VARIABLE))
        noProxy.set(owner.providers.environmentVariable(NO_PROXY_VARIABLE))
        githubToken.set(owner.providers.environmentVariable(GITHUB_TOKEN_VARIABLE))
        snapshotsFile.set(owner.layout.buildDirectory.file(SNAPSHOTS_FILE))
        documentFile.set(owner.layout.buildDirectory.file(AUDIT_DOCUMENT))
    }
}

private fun Project.registerReview(
    standards: AssuranceStandardsExtension,
    declarations: List<StandardDeclaration>,
    audit: TaskProvider<StandardsAudit>
) {
    val owner = this
    tasks.register<StandardsReview>(StandardsTaskNames.REVIEW) {
        dependsOn(audit)
        this.declarations.set(declarations)
        snapshotsFile.set(audit.flatMap { task -> task.snapshotsFile })
        lockFile.set(standards.lockFile)
        lockPath.set(standards.lockFile.map { file -> lockPathOf(owner, file.asFile) })
        today.set(owner.providers.of(CurrentDateSource::class.java) { })
    }
}

private fun Project.reuseWindowOf(assurance: AssuranceExtension): Provider<Long> {
    val window = assurance.reuseVerdictFor.map { duration -> duration.seconds }
    return providers.of(ReuseWindowSource::class.java) { parameters.windowSeconds.set(window) }
}

private fun Project.standardsCacheService(): Provider<StandardsCacheService> =
    gradle.sharedServices.registerIfAbsent(STANDARDS_CACHE_SERVICE, StandardsCacheService::class.java) {
        maxParallelUsages.set(MAX_PARALLEL_CACHE_USAGES)
    }

private fun Project.validatedDeclarations(standards: AssuranceStandardsExtension): List<StandardDeclaration> {
    val declarations = standards.declaredStandards()
    val defaultInterval = standards.reviewEvery.orNull?.seconds
    val problems = DeclarationValidator.problemsOf(declarations, defaultInterval)
    if (problems.isEmpty()) return declarations
    val listed = problems.joinToString("\n") { problem -> "- $problem" }
    throw InvalidUserDataException("assurance { standards { } } of $path is invalid:\n$listed")
}

private fun Project.registerInventory(standards: AssuranceStandardsExtension, declarations: List<StandardDeclaration>) {
    val owner = this
    val scanned = scannedFiles(standards)
    tasks.register<StandardsInventory>(StandardsTaskNames.INVENTORY) {
        this.declarations.set(declarations)
        defaultReviewEverySeconds.set(standards.reviewEvery.map { duration -> duration.seconds })
        lockFile.from(standards.lockFile)
        lockPath.set(standards.lockFile.map { file -> lockPathOf(owner, file.asFile) })
        today.set(owner.providers.of(CurrentDateSource::class.java) { })
        scanEnabled.set(standards.inventory.enabled)
        sources.from(scanned)
        baseDirectory.set(owner.layout.projectDirectory)
        documentFile.set(owner.layout.buildDirectory.file(INVENTORY_DOCUMENT))
    }
}

private fun Project.scannedFiles(standards: AssuranceStandardsExtension): FileTree {
    val inventory = standards.inventory
    val lock = standards.lockFile.get().asFile
    val lockPattern = lockPathOf(this, lock)
    val includes = inventory.includes.get()
    val excludes = GENERATED_PATHS + inventory.excludes.get() + lockPattern
    return fileTree(layout.projectDirectory) {
        include(includes)
        exclude(excludes)
    }
}

private fun lockPathOf(project: Project, lock: File): String {
    val directory = project.layout.projectDirectory.asFile
    return lock.relativeTo(directory).invariantSeparatorsPath
}
