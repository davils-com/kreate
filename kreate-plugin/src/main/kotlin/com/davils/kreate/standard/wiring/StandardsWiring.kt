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

import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.assurance.AssuranceStandardsExtension
import com.davils.kreate.assurance.task.CurrentDateSource
import com.davils.kreate.gradle.GENERATED_PATHS
import com.davils.kreate.standard.StandardsTaskNames
import com.davils.kreate.standard.model.DeclarationValidator
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.task.StandardsInventory
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.kotlin.dsl.register

private const val INVENTORY_DOCUMENT: String = "kreate/assurance/documents/standards-inventory.json"

internal fun Project.registerStandards(assurance: AssuranceExtension) {
    val standards = assurance.standards
    val declarations = validatedDeclarations(standards)
    registerInventory(standards, declarations)
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

private fun lockPathOf(project: Project, lock: java.io.File): String {
    val directory = project.layout.projectDirectory.asFile
    return lock.relativeTo(directory).invariantSeparatorsPath
}
