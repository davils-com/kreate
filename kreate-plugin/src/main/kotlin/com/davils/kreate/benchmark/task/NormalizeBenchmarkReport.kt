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

package com.davils.kreate.benchmark.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

private const val CAN_JUDGE_TIMESTAMPED_SOURCE_UP_TO_DATE = false

@DisableCachingByDefault(because = "Reads a directory whose name changes on every run")
internal abstract class NormalizeBenchmarkReport : KreateTask(
    "Copies the newest benchmark report to a stable location.",
    KreateTaskGroup.BENCHMARK
) {
    @get:Internal
    public abstract val timestampedReportsDirectory: DirectoryProperty

    @get:Input
    public abstract val profile: Property<String>

    @get:OutputDirectory
    public abstract val normalizedDirectory: DirectoryProperty

    init {
        outputs.upToDateWhen { CAN_JUDGE_TIMESTAMPED_SOURCE_UP_TO_DATE }
    }

    @TaskAction
    public fun execute() {
        val source = timestampedReportsDirectory.get().asFile
        val newestRun = newestRunDirectory(source) ?: throw GradleException(missingRunMessage(source))

        val target = normalizedDirectory.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        val runEntries = newestRun.listFiles().orEmpty()
        val reports = runEntries.filter { it.isFile }
        reports.forEach { report -> report.copyTo(target.resolve(report.name), overwrite = true) }

        logger.lifecycle(
            "Normalized ${reports.size} benchmark report(s) from ${newestRun.name} to ${target.path}."
        )
    }

    private fun missingRunMessage(source: File): String =
        listOf(
            "No benchmark report was found for profile '${profile.get()}'.",
            "",
            "Expected a run below ${source.path}.",
            "Run the benchmarks first — the report is written by kotlinx-benchmark, " +
                "not by this task."
        ).joinToString("\n")

    private fun newestRunDirectory(reportsDirectory: File): File? {
        val entries = reportsDirectory.listFiles().orEmpty()
        val runDirectories = entries.filter { it.isDirectory }
        return runDirectories.maxWithOrNull(compareBy({ it.lastModified() }, { it.name }))
    }
}
