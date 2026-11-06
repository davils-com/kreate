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

import com.davils.kreate.assurance.AssuranceExtension
import com.davils.kreate.assurance.AssuranceIgnoreSpec
import com.davils.kreate.assurance.AssuranceTaskNames
import com.davils.kreate.assurance.task.AssuranceBaselineCreateTask
import com.davils.kreate.assurance.task.AssuranceBaselinePruneTask
import com.davils.kreate.assurance.task.AssuranceEvaluationTask
import com.davils.kreate.assurance.task.AssuranceReportTask
import com.davils.kreate.assurance.task.AssuranceVerdictTask
import com.davils.kreate.assurance.task.CurrentDateSource
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.assurance.task.IgnoreRuleInput
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val REPORT_DIRECTORY: String = "kreate/assurance/reports"

private const val RESULT_FILE: String = "kreate/assurance/result.json"

internal fun Project.initializeAssurance(assurance: AssuranceExtension) {
    if (!assurance.enabled.get()) return

    val rules = assurance.declaredIgnoreRules().map { (id, spec) -> ignoreRuleInputOf(id, spec) }

    val report = tasks.register<AssuranceReportTask>(AssuranceTaskNames.REPORT) {
        configureEvaluation(assurance, rules)
        formats.set(assurance.reports.formats)
        outputDirectory.set(layout.buildDirectory.dir(REPORT_DIRECTORY))
    }

    val verdict = tasks.register<AssuranceVerdictTask>(AssuranceTaskNames.CHECK) {
        configureEvaluation(assurance, rules)
        resultFile.set(layout.buildDirectory.file(RESULT_FILE))
        finalizedBy(report)
    }

    tasks.register<AssuranceBaselineCreateTask>(AssuranceTaskNames.BASELINE_CREATE) {
        configureEvaluation(assurance, rules)
        target.set(assurance.baseline.file)
    }

    tasks.register<AssuranceBaselinePruneTask>(AssuranceTaskNames.BASELINE_PRUNE) {
        configureEvaluation(assurance, rules)
        target.set(assurance.baseline.file)
    }

    if (!assurance.runOnCheck.get()) return

    tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
        dependsOn(verdict)
    }
}

private fun AssuranceEvaluationTask.configureEvaluation(assurance: AssuranceExtension, rules: List<IgnoreRuleInput>) {
    documents.from(assurance.externalFindings)
    documents.from(project.provider { project.collectedDocuments() })
    baselineFile.from(assurance.baseline.file)
    failOn.set(assurance.failOn)
    minSeverity.set(assurance.minSeverity)
    reportOnly.set(assurance.reportOnly)
    failOnStale.set(assurance.baseline.failOnStale)
    forbidIgnoreOf.set(assurance.forbidIgnoreOf)
    maxAdvisoryAge.set(assurance.maxAdvisoryAge)
    maxBaselineSeverity.set(assurance.baseline.maxSeverity)
    ignoreRules.set(rules)
    today.set(project.providers.of(CurrentDateSource::class.java) { })
    projectPath.set(project.path)
}

private fun Project.collectedDocuments(): List<RegularFileProperty> {
    val collectors = tasks.withType(FindingCollectTask::class.java)
    return collectors.map { collector -> collector.documentFile }
}

private fun ignoreRuleInputOf(id: String, spec: AssuranceIgnoreSpec): IgnoreRuleInput {
    val reason = spec.reason
    if (reason.isNullOrBlank()) {
        throw InvalidUserDataException("assurance { ignore(\"$id\") } needs a reason.")
    }

    return IgnoreRuleInput(id, reason, spec.module, validatedDate(id, spec.expires))
}

private fun validatedDate(id: String, expires: String?): String? {
    val date = expires ?: return null
    return try {
        LocalDate.parse(date).toString()
    } catch (cause: DateTimeParseException) {
        throw InvalidUserDataException("assurance { ignore(\"$id\") } needs a YYYY-MM-DD date.", cause)
    }
}
