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

import com.davils.kreate.KREATE_VERSION
import com.davils.kreate.assurance.Severity
import com.davils.kreate.assurance.baseline.BaselineEntry
import com.davils.kreate.assurance.baseline.BaselineJson
import com.davils.kreate.assurance.baseline.BaselineMatcher
import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingDocumentJson
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.policy.FindingFilter
import com.davils.kreate.assurance.policy.VerdictRules
import com.davils.kreate.assurance.report.AssuranceEvaluation
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@DisableCachingByDefault(because = "Base task class of the assurance tasks")
internal abstract class AssuranceEvaluationTask(taskDescription: String) :
    KreateTask(taskDescription, KreateTaskGroup.ASSURANCE) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val documents: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val baselineFile: ConfigurableFileCollection

    @get:Input
    public abstract val failOn: Property<Severity>

    @get:Input
    public abstract val minSeverity: Property<Severity>

    @get:Input
    public abstract val reportOnly: Property<Boolean>

    @get:Input
    public abstract val failOnStale: Property<Boolean>

    @get:Input
    public abstract val forbidIgnoreOf: SetProperty<Severity>

    @get:Input
    public abstract val ignoreRules: ListProperty<IgnoreRuleInput>

    @get:Input
    @get:Optional
    public abstract val maxBaselineSeverity: Property<Severity>

    @get:Input
    public abstract val maxAdvisoryAge: Property<Duration>

    @get:Input
    public abstract val today: Property<String>

    @get:Input
    public abstract val projectPath: Property<String>

    protected fun readDocuments(): List<FindingDocument> = documents.files.map { file -> readDocument(file) }

    protected fun readBaseline(): List<BaselineEntry> {
        val file = baselineFile.files.firstOrNull { candidate -> candidate.isFile } ?: return emptyList()
        return BaselineJson.parse(file.readText(), file.name)
    }

    protected fun completeSourcesOf(documents: List<FindingDocument>): Set<FindingSource> {
        val complete = documents.filter { document -> document.status == DocumentStatus.COMPLETE }
        return complete.map { document -> document.source }.toSet()
    }

    protected fun findingsOf(documents: List<FindingDocument>): List<Finding> =
        documents.flatMap { document -> document.findings }

    protected fun evaluate(): AssuranceEvaluation {
        val readDocuments = readDocuments()
        val filter = FindingFilter(
            minSeverity = minSeverity.get(),
            rules = ignoreRules.get().map { rule -> rule.toRule() },
            today = LocalDate.parse(today.get()),
            baseline = BaselineMatcher(readBaseline(), maxBaselineSeverity.orNull),
            unignorable = forbidIgnoreOf.get()
        )
        val outcome = filter.apply(findingsOf(readDocuments), completeSourcesOf(readDocuments))
        val rules = VerdictRules(failOn.get(), reportOnly.get(), failOnStale.get(), maxAdvisoryAge.get())
        val now = Instant.now()

        return AssuranceEvaluation(
            project = projectPath.get(),
            toolVersion = KREATE_VERSION,
            generatedAt = now,
            documents = readDocuments,
            outcome = outcome,
            verdict = rules.decide(readDocuments, outcome, now)
        )
    }

    private fun readDocument(file: File): FindingDocument {
        if (!file.isFile) throw AssuranceInputException("The finding document ${file.path} does not exist.")
        return FindingDocumentJson.parse(file.readText(), file.name)
    }
}
