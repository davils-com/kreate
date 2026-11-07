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
package com.davils.kreate.governance.task

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocumentJson
import com.davils.kreate.governance.catalog.TextDiff
import com.davils.kreate.governance.fix.DependencyFixer
import com.davils.kreate.governance.fix.VulnerableVersion
import com.davils.kreate.governance.policy.DeclaredDependency
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

private const val CATALOG_PATH: String = "gradle/libs.versions.toml"

@UntrackedTask(because = "It edits the version catalog on request")
internal abstract class DependencyFix : KreateTask(
    "Raises vulnerable and dynamic versions in gradle/libs.versions.toml, as a diff unless --apply.",
    KreateTaskGroup.ASSURANCE
) {
    @get:Internal
    @get:Option(option = "apply", description = "Writes the changes to the catalog instead of showing them.")
    public abstract val apply: Property<Boolean>

    @get:Internal
    public abstract val catalogFile: RegularFileProperty

    @get:Internal
    public abstract val advisoryDocuments: ConfigurableFileCollection

    @get:Internal
    public abstract val declared: ListProperty<DeclaredDependency>

    @get:Internal
    public abstract val diffFile: RegularFileProperty

    @TaskAction
    public fun fix() {
        val file = catalogFile.get().asFile
        if (!file.isFile) throw InvalidUserDataException("The build has no $CATALOG_PATH to fix.")
        val before = file.readText()
        val resolved = declared.get().mapNotNull { dependency ->
            dependency.selected?.let { version -> "${dependency.group}:${dependency.name}" to version }
        }
        val result = DependencyFixer(vulnerableVersions(), resolved.toMap()).fix(before)
        result.skipped.forEach { reason -> logger.warn("Skipped: $reason") }
        if (result.changes.isEmpty()) return logger.lifecycle("Nothing in $CATALOG_PATH needs a fix.")

        val diff = TextDiff.unified(CATALOG_PATH, before, result.text)
        val output = diffFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(diff)
        result.changes.forEach { change -> logger.lifecycle("  $change") }
        if (!apply.getOrElse(false)) {
            return logger.lifecycle("The diff is in ${output.path}. Run with --apply to write $CATALOG_PATH.")
        }
        file.writeText(result.text)
        logger.lifecycle("Wrote ${result.changes.size} change(s) to $CATALOG_PATH.")
    }

    private fun vulnerableVersions(): List<VulnerableVersion> {
        val documents = advisoryDocuments.filter { file -> file.isFile }
        val findings = documents.flatMap { file -> FindingDocumentJson.parse(file.readText(), file.path).findings }
        return findings.mapNotNull { finding -> vulnerableOf(finding) }
    }

    private fun vulnerableOf(finding: Finding): VulnerableVersion? {
        val module = finding.module ?: return null
        val version = finding.version ?: return null
        val fixed = finding.fixedVersion ?: return null
        return VulnerableVersion(module, version, fixed)
    }
}
