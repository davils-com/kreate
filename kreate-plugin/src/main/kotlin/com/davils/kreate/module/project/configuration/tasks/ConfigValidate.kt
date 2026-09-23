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

package com.davils.kreate.module.project.configuration.tasks

import com.davils.kreate.KreateTasks
import com.davils.kreate.jobs.Task
import com.davils.kreate.module.project.configuration.DeclaredSchema
import com.davils.kreate.module.project.configuration.SchemaReflection
import com.davils.kreate.module.project.configuration.ValidationOutcome
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

/**
 * Fails the build when one of the repository's own configuration files would not load.
 *
 * A dry run rather than a load: nothing is written, no backup is made, no watcher is started and no
 * handle is opened, so the check does not change the thing it is checking. It runs over every
 * declared opening rather than stopping at the first failure, because a run over forty files that
 * reports one of them is a run somebody has to repeat thirty-nine times.
 *
 * @since 3.1.0
 */
@DisableCachingByDefault(
    because = "Runs the project's own code against files outside its inputs, so a cached result would lie"
)
public abstract class ConfigValidate : Task(
    "Reports what loading this repository's own configuration files would do.",
    KreateTasks.ConfigurationSchema.GROUP
) {
    /**
     * The runtime classpath the reports are produced on.
     *
     * @since 3.1.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val runtimeClasspath: ConfigurableFileCollection

    /**
     * The configuration files the dry run reads, so a change to one runs the check again.
     *
     * @since 3.1.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val configurationFiles: ConfigurableFileCollection

    /**
     * Where the reports are reached, as a name, a holder and an accessor.
     *
     * @since 3.1.0
     */
    @get:Input
    public abstract val reports: Property<DeclaredSchema>

    /**
     * What the run found, written for whoever reads a failed build afterwards.
     *
     * @since 3.1.0
     */
    @get:OutputFile
    public abstract val reportFile: RegularFileProperty

    /**
     * Runs the dry run and refuses a repository whose own configuration would not load.
     *
     * @return Unit
     * @throws GradleException When any declared opening would fail to load.
     * @since 3.1.0
     */
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

        throw GradleException(
            (listOf("This repository's own configuration would not load.", "") + linesOf(broken))
                .joinToString("\n")
        )
    }

    private fun linesOf(broken: List<ValidationOutcome>): List<String> = broken.flatMap { outcome ->
        listOf("  ${outcome.path}") + outcome.problems.map { problem -> "    $problem" }
    }

    private fun write(outcomes: List<ValidationOutcome>) {
        val file = reportFile.get().asFile
        file.parentFile.mkdirs()
        val lines = outcomes.map { outcome ->
            val state = if (outcome.isLoadable) "loads" else "fails"
            "${outcome.path}: $state"
        }
        file.writeText(lines.joinToString("\n"))
    }
}
