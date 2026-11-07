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
package com.davils.kreate.assurance.task

import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.ingest.ApiDumpIngest
import com.davils.kreate.assurance.ingest.CoverageIngest
import com.davils.kreate.assurance.ingest.SarifIngest
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant
import java.util.Locale

@CacheableTask
internal abstract class QualityIngest : FindingCollectTask(
    "Reads the reports of Detekt, the coverage and the API check into the gate.",
    KreateTaskGroup.ASSURANCE
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val sarifReports: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val coverageReport: ConfigurableFileCollection

    @get:Input
    @get:Optional
    public abstract val minLineCoverage: Property<Int>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val expectedDump: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val actualDump: ConfigurableFileCollection

    @get:Internal
    public abstract val projectDirectory: DirectoryProperty

    @TaskAction
    public fun ingest() {
        val root = projectDirectory.get().asFile
        val sarif = existing(sarifReports)
        val coverage = existing(coverageReport).firstOrNull()
        val detekt = sarif.flatMap { file -> SarifIngest.findingsOf(file.readText(), root.toURI().toString()) }
        val lines = coverage?.let { file -> CoverageIngest.lineCoverageOf(file.readBytes()) }
        val covered = CoverageIngest.findingsOf(lines, minLineCoverage.orNull)
        val api = apiFindings(root)
        val findings = detekt + covered + api
        val read = sarif.isNotEmpty() || coverage != null || existing(expectedDump).isNotEmpty()
        val status = statusOf(read)
        writeDocument(FindingDocument(FindingSource.QUALITY, status, Instant.now(), findings))
        val measured = lines?.let { value -> String.format(Locale.ROOT, "%.1f %%", value) } ?: "none"
        logger.lifecycle("Read ${sarif.size} Detekt report(s), coverage $measured: ${findings.size} finding(s).")
    }

    private fun apiFindings(root: File): List<Finding> {
        val expected = existing(expectedDump).firstOrNull() ?: return emptyList()
        val actual = existing(actualDump).firstOrNull()
        val path = expected.relativeTo(root).invariantSeparatorsPath
        return ApiDumpIngest.findingsOf(expected.readText(), actual?.readText(), path)
    }

    private fun existing(files: ConfigurableFileCollection): List<File> = files.filter { file -> file.isFile }.toList()

    private fun statusOf(read: Boolean): DocumentStatus {
        if (read) return DocumentStatus.COMPLETE
        return DocumentStatus.SKIPPED
    }
}
