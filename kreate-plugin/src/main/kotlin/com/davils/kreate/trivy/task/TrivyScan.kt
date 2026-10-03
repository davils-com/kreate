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

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File

private const val TRIVY_LIST_SEPARATOR = ","
private const val TRIVY_OUTPUT_FORMAT = "table"

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivyScan(
    taskDescription: String,
    private val scanner: String,
    private val exec: ExecOperations
) : KreateTask(taskDescription, KreateTaskGroup.TRIVY) {
    @get:Input
    public abstract val severity: ListProperty<String>

    @get:Input
    public abstract val failOnFindings: Property<Boolean>

    protected open fun scannerArguments(): List<String> = emptyList()

    protected fun scanReportsFindings(target: File): Boolean {
        val result = exec.exec {
            isIgnoreExitValue = true
            commandLine(trivyArguments(target))
        }
        return trivyReportedFindings(result.exitValue, target)
    }

    protected fun joinForTrivy(values: List<String>): String = values.joinToString(TRIVY_LIST_SEPARATOR)

    private fun trivyArguments(target: File): List<String> {
        val scanArguments = listOf(resolveTrivyCommand(), "fs", "--scanners", scanner)
        val reportArguments = listOf(
            "--severity", joinForTrivy(severity.get()),
            "--exit-code", findingsExitCode().toString(),
            "--format", TRIVY_OUTPUT_FORMAT
        )
        return scanArguments + scannerArguments() + reportArguments + target.absolutePath
    }

    private fun findingsExitCode(): Int {
        if (!failOnFindings.get()) return TRIVY_CLEAN_EXIT_CODE
        return TRIVY_FINDINGS_EXIT_CODE
    }
}
