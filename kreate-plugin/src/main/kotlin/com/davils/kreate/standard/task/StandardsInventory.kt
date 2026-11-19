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
package com.davils.kreate.standard.task

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.standard.inventory.LockChecks
import com.davils.kreate.standard.inventory.ReferenceScanner
import com.davils.kreate.standard.inventory.ScannedFile
import com.davils.kreate.standard.lock.StandardsLock
import com.davils.kreate.standard.lock.StandardsLockJson
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@CacheableTask
internal abstract class StandardsInventory : FindingCollectTask(
    "Checks that every declared standard was reviewed, that no review is due and that the code names no other.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Input
    public abstract val declarations: ListProperty<StandardDeclaration>

    @get:Input
    @get:Optional
    public abstract val defaultReviewEverySeconds: Property<Long>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val lockFile: ConfigurableFileCollection

    @get:Input
    public abstract val lockPath: Property<String>

    @get:Input
    public abstract val today: Property<String>

    @get:Input
    public abstract val scanEnabled: Property<Boolean>

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sources: ConfigurableFileCollection

    @get:Internal
    public abstract val baseDirectory: DirectoryProperty

    @TaskAction
    public fun inventory() {
        val lock = try {
            readLock()
        } catch (cause: AssuranceInputException) {
            val reason = cause.message.orEmpty()
            writeDocument(documentOf(DocumentStatus.INCOMPLETE, reason = reason))
            logger.warn("The inventory of the standards is incomplete: $reason")
            return
        }
        val declared = declarations.get()
        val scanned = scanEnabled.get()
        if (declared.isEmpty() && lock.entries.isEmpty() && !scanned) {
            writeDocument(documentOf(DocumentStatus.SKIPPED))
            return
        }
        val findings = lockFindings(lock, declared) + referenceFindings(declared, scanned)
        writeDocument(documentOf(DocumentStatus.COMPLETE, findings))
        logger.lifecycle("Checked ${declared.size} standard(s): ${findings.size} finding(s).")
        findings.forEach { finding -> logger.lifecycle("${finding.severity.name} ${finding.ruleId}: ${finding.title}") }
    }

    private fun documentOf(
        status: DocumentStatus,
        findings: List<Finding> = emptyList(),
        reason: String? = null
    ): FindingDocument = FindingDocument(FindingSource.STANDARDS_INVENTORY, status, Instant.now(), findings, reason)

    private fun readLock(): StandardsLock {
        val file = lockFile.files.firstOrNull { candidate -> candidate.isFile } ?: return StandardsLock()
        return StandardsLockJson.parse(file.readText(), lockPath.get())
    }

    private fun lockFindings(lock: StandardsLock, declared: List<StandardDeclaration>): List<Finding> {
        val defaultInterval = defaultReviewEverySeconds.orNull?.let(Duration::ofSeconds)
        val checks = LockChecks(declared, defaultInterval, lockPath.get())
        return checks.findingsOf(lock, LocalDate.parse(today.get()))
    }

    private fun referenceFindings(declared: List<StandardDeclaration>, scanned: Boolean): List<Finding> {
        if (!scanned) return emptyList()
        val base = baseDirectory.get().asFile
        val files = sources.files.map { file -> ScannedFile(file.relativeTo(base).invariantSeparatorsPath, file) }
        val ids = declared.map { declaration -> declaration.id }.toSet()
        return ReferenceScanner(ids).findingsOf(files)
    }
}
