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

package com.davils.kreate.configuration.task

import com.davils.kreate.configuration.schema.DeclaredSchema
import com.davils.kreate.configuration.schema.SchemaReflection
import com.davils.kreate.configuration.schema.ValidationOutcome
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

private const val LOADS_STATE = "loads"
private const val FAILS_STATE = "fails"

@DisableCachingByDefault(
    because = "Runs the project's own code against files outside its inputs, so a cached result would lie"
)
internal abstract class ConfigValidate : KreateTask(
    "Reports what loading this repository's own configuration files would do.",
    KreateTaskGroup.CONFIGURATION
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val configurationFiles: ConfigurableFileCollection

    @get:Input
    public abstract val reports: Property<DeclaredSchema>

    @get:OutputFile
    public abstract val reportFile: RegularFileProperty

    @TaskAction
    public fun execute() {
        val declared = reports.get()
        val outcomes = SchemaReflection(runtimeClasspath.files).use { reflection ->
            val produced = reflection.declarationOf(declared.holder, declared.accessor)
            reflection.outcomesOf(produced)
        }

        write(outcomes)
        val broken = outcomes.filterNot { outcome -> outcome.isLoadable }
        if (broken.isEmpty()) return

        val failure = listOf("This repository's own configuration would not load.", "") + linesOf(broken)
        throw GradleException(failure.joinToString("\n"))
    }

    private fun linesOf(broken: List<ValidationOutcome>): List<String> = broken.flatMap { outcome ->
        listOf("  ${outcome.path}") + outcome.problems.map { problem -> "    $problem" }
    }

    private fun write(outcomes: List<ValidationOutcome>) {
        val file = reportFile.get().asFile
        file.parentFile.mkdirs()
        val lines = outcomes.map { outcome -> "${outcome.path}: ${stateOf(outcome)}" }
        file.writeText(lines.joinToString("\n"))
    }

    private fun stateOf(outcome: ValidationOutcome): String {
        if (outcome.isLoadable) return LOADS_STATE
        return FAILS_STATE
    }
}
