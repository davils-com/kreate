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
import com.davils.kreate.api.ApiValidationTaskNames
import com.davils.kreate.assurance.task.QualityIngest
import dev.detekt.gradle.Detekt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

private const val INGEST_TASK: String = "kreateAssuranceIngest"

private const val DOCUMENT_FILE: String = "kreate/assurance/documents/quality.json"

private const val COVERAGE_XML_TASK: String = "koverXmlReport"

private const val ACTUAL_DUMP_DIRECTORY: String = "kreate/api"

internal fun Project.registerQualityIngest(extension: KreateExtension) {
    val assurance = extension.assurance
    if (!assurance.reports.ingestQuality.get()) return
    val project = extension.project
    val owner = this
    val api = project.apiValidation
    val coverage = project.coverage

    tasks.register<QualityIngest>(INGEST_TASK) {
        if (project.detekt.enabled.get()) {
            val detektTasks = owner.tasks.withType<Detekt>()
            sarifReports.from(owner.provider { detektTasks.map { task -> task.reports.sarif.outputLocation } })
            mustRunAfter(detektTasks)
        }
        if (coverage.enabled.get()) {
            coverageReport.from(coverage.reports.xml.file)
            minLineCoverage.set(coverage.verify.minLineCoverage)
            mustRunAfter(owner.tasks.matching { task -> task.name == COVERAGE_XML_TASK })
        }
        if (api.enabled.get()) {
            expectedDump.from(api.apiDirectory.file(api.dumpFileName))
            val actualPath = api.dumpFileName.map { name -> "$ACTUAL_DUMP_DIRECTORY/$name" }
            actualDump.from(owner.layout.buildDirectory.file(actualPath))
            mustRunAfter(ApiValidationTaskNames.CHECK)
        }
        projectDirectory.set(owner.layout.projectDirectory)
        documentFile.set(owner.layout.buildDirectory.file(DOCUMENT_FILE))
    }
}
