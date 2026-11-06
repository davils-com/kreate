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
package com.davils.kreate.trivy.task

import com.davils.kreate.assurance.finding.AssuranceInputException
import com.davils.kreate.assurance.finding.DocumentStatus
import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.assurance.finding.FindingDocument
import com.davils.kreate.assurance.finding.FindingSource
import com.davils.kreate.assurance.policy.REPORT_ONLY_ATTRIBUTE
import com.davils.kreate.assurance.task.FindingCollectTask
import com.davils.kreate.task.KreateTaskGroup
import com.davils.kreate.tooling.ExecOperationsToolRunner
import com.davils.kreate.tooling.ToolInvocation
import com.davils.kreate.tooling.ToolNotFoundException
import com.davils.kreate.tooling.ToolResult
import com.davils.kreate.tooling.ToolStatus
import com.davils.kreate.trivy.report.TrivyReportParser
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.time.Duration
import java.time.Instant

private const val TRIVY_LIST_SEPARATOR: String = ","

private const val TRIVY_OUTPUT_FORMAT: String = "json"

private const val SUCCESS_EXIT_CODE: Int = 0

private const val MAX_STDERR_CHARACTERS: Int = 2000

private val SCAN_TIMEOUT: Duration = Duration.ofMinutes(30)

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivyScan(
    taskDescription: String,
    private val scanner: String,
    exec: ExecOperations
) : FindingCollectTask(taskDescription, KreateTaskGroup.TRIVY) {
    private val runner = ExecOperationsToolRunner(exec)

    @get:Input
    public abstract val severity: ListProperty<String>

    @get:Input
    public abstract val failOnFindings: Property<Boolean>

    @get:Input
    public abstract val gated: Property<Boolean>

    protected open fun scannerArguments(): List<String> = emptyList()

    protected fun joinForTrivy(values: List<String>): String = values.joinToString(TRIVY_LIST_SEPARATOR)

    protected fun runTrivy(target: TrivyTarget): TrivyRun {
        val report = temporaryDir.resolve("report.json")
        report.delete()

        return try {
            val invocation = ToolInvocation(trivyArguments(target.file, report), temporaryDir, timeout = SCAN_TIMEOUT)
            outcomeOf(runner.run(invocation), target, report)
        } catch (cause: ToolNotFoundException) {
            TrivyRun(emptyList(), cause.message.orEmpty())
        }
    }

    protected fun publish(runs: List<TrivyRun>, findingsMessage: (List<Finding>) -> String) {
        val failures = runs.mapNotNull { run -> run.failure }
        val findings = runs.flatMap { run -> run.findings }
        writeDocument(documentOf(findings, failures))
        findings.forEach { finding -> logger.lifecycle(describe(finding)) }

        if (gated.get()) return
        if (failures.isNotEmpty()) throw GradleException(failures.joinToString("\n"))
        if (findings.isEmpty() || !failOnFindings.get()) return

        throw GradleException(findingsMessage(findings))
    }

    private fun trivyArguments(target: File, report: File): List<String> {
        val scanArguments = listOf(resolveTrivyCommand(), "fs", "--quiet", "--scanners", scanner)
        val reportArguments = listOf(
            "--severity", joinForTrivy(severity.get()),
            "--exit-code", SUCCESS_EXIT_CODE.toString(),
            "--format", TRIVY_OUTPUT_FORMAT,
            "--output", report.absolutePath
        )
        return scanArguments + scannerArguments() + reportArguments + target.absolutePath
    }

    private fun outcomeOf(result: ToolResult, target: TrivyTarget, report: File): TrivyRun {
        val name = target.file.invariantSeparatorsPath
        val failure = failureOf(result, name)
        if (failure != null) return TrivyRun(emptyList(), failure)
        if (!report.isFile) return TrivyRun(emptyList(), "Trivy wrote no report for $name.")

        return try {
            TrivyRun(TrivyReportParser.parse(report.readText(), target.pathOf))
        } catch (cause: AssuranceInputException) {
            TrivyRun(emptyList(), "${cause.message} ($name)")
        }
    }

    private fun failureOf(result: ToolResult, name: String): String? {
        val output = result.stderr.text.takeLast(MAX_STDERR_CHARACTERS)
        return when (result.status) {
            ToolStatus.EXITED -> exitFailureOf(result.exitCode, name, output)
            ToolStatus.TIMED_OUT -> "Trivy did not finish scanning $name within $SCAN_TIMEOUT."
            ToolStatus.NOT_STARTED -> "Trivy could not be started for $name: $output"
        }
    }

    private fun exitFailureOf(exitCode: Int?, name: String, output: String): String? {
        if (exitCode == SUCCESS_EXIT_CODE) return null
        return "Trivy did not finish scanning $name: it exited with $exitCode. " +
            "This is a failure of Trivy itself, not a finding.\n$output"
    }

    private fun documentOf(findings: List<Finding>, failures: List<String>): FindingDocument {
        val status = statusOf(failures)
        val marked = findings.map(::markedForGate)
        return FindingDocument(
            source = FindingSource.TRIVY,
            status = status,
            evaluatedAt = Instant.now(),
            findings = marked,
            reason = failures.firstOrNull()
        )
    }

    private fun statusOf(failures: List<String>): DocumentStatus {
        if (failures.isEmpty()) return DocumentStatus.COMPLETE
        return DocumentStatus.INCOMPLETE
    }

    private fun markedForGate(finding: Finding): Finding {
        if (failOnFindings.get()) return finding
        return finding.copy(attributes = finding.attributes + (REPORT_ONLY_ATTRIBUTE to true.toString()))
    }

    private fun describe(finding: Finding): String {
        val place = finding.location?.path.orEmpty()
        val subject = finding.module ?: place
        return "${finding.severity.name} ${finding.ruleId} $subject: ${finding.title}"
    }
}
