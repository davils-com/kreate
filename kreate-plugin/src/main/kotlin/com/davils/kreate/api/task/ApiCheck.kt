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

package com.davils.kreate.api.task

import com.davils.kreate.api.abi.AbiDiff
import com.davils.kreate.api.abi.AbiFilterOptions
import com.davils.kreate.api.abi.readAbiDump
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
internal abstract class ApiCheck : KreateTask(
    "Verifies that the public binary interface matches the checked-in .api dump.",
    KreateTaskGroup.API
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val classDirectories: ConfigurableFileCollection

    @get:Input
    public abstract val nonPublicMarkers: SetProperty<String>

    @get:Input
    public abstract val ignoredPackages: SetProperty<String>

    @get:Input
    public abstract val ignoredClasses: SetProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val expectedDumpFile: ConfigurableFileCollection

    @get:Input
    public abstract val dumpTaskPath: Property<String>

    @get:Input
    public abstract val projectPath: Property<String>

    @get:OutputFile
    public abstract val actualDumpFile: RegularFileProperty

    @TaskAction
    public fun execute() {
        val options = AbiFilterOptions(
            nonPublicMarkers = nonPublicMarkers.get(),
            ignoredPackages = ignoredPackages.get(),
            ignoredClasses = ignoredClasses.get()
        )
        val actual = renderAbiDump(classDirectories, options)

        val actualFile = actualDumpFile.get().asFile
        actualFile.parentFile.mkdirs()
        actualFile.writeText(actual)

        val expectedFile = expectedDumpFile.files.singleOrNull()?.takeIf { file -> file.isFile }
            ?: throw GradleException(missingDumpMessage())

        val diff = AbiDiff.render(expected = readAbiDump(expectedFile), actual = actual)
            ?: return

        throw GradleException(changedInterfaceMessage(diff, expectedFile, actualFile))
    }

    private fun missingDumpMessage(): String = listOf(
        "No binary interface dump has been recorded for project " +
            "'${projectPath.get()}' yet.",
        "",
        "Create it and commit the result:",
        "",
        "    ./gradlew ${dumpTaskPath.get()}"
    ).joinToString("\n")

    private fun changedInterfaceMessage(diff: String, expectedFile: File, actualFile: File): String = listOf(
        "The public binary interface of project '${projectPath.get()}' changed.",
        "",
        diff,
        "",
        "If the change is intended, record it and commit the result:",
        "",
        "    ./gradlew ${dumpTaskPath.get()}",
        "",
        "Expected: ${expectedFile.path}",
        "Actual:   ${actualFile.path}"
    ).joinToString("\n")
}
