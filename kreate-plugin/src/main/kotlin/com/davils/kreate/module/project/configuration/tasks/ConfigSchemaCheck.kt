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
import com.davils.kreate.module.project.configuration.BREAKING_KIND
import com.davils.kreate.module.project.configuration.DeclaredSchema
import com.davils.kreate.module.project.configuration.SchemaDifference
import com.davils.kreate.module.project.configuration.SchemaReflection
import com.davils.kreate.module.project.configuration.exportFileName
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Fails the build when a configuration schema changed in a way that stops a deployed document
 * loading.
 *
 * **The version is what decides, and the library decides it.** A change that would stop an existing
 * document loading is breaking when the schema version has not moved, because nothing will run to
 * repair it, and merely a migration when it has. This task asks and reports; it does not have an
 * opinion of its own about what a change means.
 *
 * @since 3.1.0
 */
@DisableCachingByDefault(
    because = "Runs the project's own code to read a declaration, which a cached result would skip"
)
public abstract class ConfigSchemaCheck : Task(
    "Verifies that no configuration schema changed in a way that breaks a deployed document.",
    KreateTasks.ConfigurationSchema.GROUP
) {
    /**
     * The runtime classpath the declarations are read out of.
     *
     * @since 3.1.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val runtimeClasspath: ConfigurableFileCollection

    /**
     * Where each declaration is reached, as a name, a holder and an accessor.
     *
     * @since 3.1.0
     */
    @get:Input
    public abstract val schemas: ListProperty<DeclaredSchema>

    /**
     * The directory holding the checked-in exports.
     *
     * Declared as a file collection rather than an input directory, because the directory
     * legitimately does not exist before the first dump and `@InputDirectory` fails the build on a
     * missing one before the task action ever runs - which would report the absence without saying
     * what to do about it.
     *
     * @since 3.1.0
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val expectedExports: ConfigurableFileCollection

    /**
     * Where the checked-in exports are.
     *
     * Internal rather than an input: [expectedExports] is what decides whether this task is up to
     * date, and declaring the same directory twice would only give Gradle two answers to one
     * question.
     *
     * @since 3.1.0
     */
    @get:Internal
    public abstract val schemaDirectory: DirectoryProperty

    /**
     * The path of the task that regenerates the exports, used in the failure message.
     *
     * @since 3.1.0
     */
    @get:Input
    public abstract val dumpTaskPath: Property<String>

    /**
     * A marker recording that the check ran, so Gradle can call it up to date.
     *
     * @since 3.1.0
     */
    @get:OutputFile
    public abstract val outcomeFile: RegularFileProperty

    /**
     * Compares every declared schema against its checked-in export.
     *
     * @return Unit
     * @throws GradleException When an export is missing or a change would break a deployed document.
     * @since 3.1.0
     */
    @TaskAction
    public fun execute() {
        val directory = schemaDirectory.get().asFile
        val breaking = mutableListOf<String>()

        SchemaReflection(runtimeClasspath.files).use { reflection ->
            schemas.get().forEach { declared ->
                val export = requireExport(directory, declared)
                val schema = reflection.declarationOf(declared.holder, declared.accessor)
                val differences = reflection.differencesBetween(schema, export.readText())
                breaking += breakingLinesOf(declared, differences)
            }
        }

        recordOutcome(breaking)
        if (breaking.isEmpty()) return

        throw GradleException(failureOf(breaking))
    }

    /**
     * The whole refusal, assembled line by line.
     *
     * Line by line rather than as a raw string, for the reason the API check does the same:
     * `trimIndent` measures the indentation of the interpolated result, so the unindented lines the
     * library produced would stop every surrounding line from being trimmed.
     */
    private fun failureOf(breaking: List<String>): String {
        val opening = listOf(
            "A configuration schema changed in a way that breaks documents already written against it.",
            ""
        )
        val remedy = listOf(
            "",
            "Raise the schema version and add the migration that repairs a stored document, or,",
            "if the change really is compatible, record the new export and commit the result:",
            "",
            "    ./gradlew ${dumpTaskPath.get()}"
        )

        return (opening + breaking + remedy).joinToString("\n")
    }

    private fun requireExport(directory: File, declared: DeclaredSchema): File {
        val export = directory.resolve(exportFileName(declared.name))
        if (export.isFile) return export

        throw GradleException(
            listOf(
                "No export has been recorded for the configuration schema '${declared.name}' yet.",
                "",
                "Create it and commit the result:",
                "",
                "    ./gradlew ${dumpTaskPath.get()}",
                "",
                "Expected: ${export.path}"
            ).joinToString("\n")
        )
    }

    private fun breakingLinesOf(
        declared: DeclaredSchema,
        differences: List<SchemaDifference>
    ): List<String> {
        val broken = differences.filter { difference -> difference.kind == BREAKING_KIND }

        return broken.map { difference -> "  ${declared.name}: ${difference.description}" }
    }

    private fun recordOutcome(breaking: List<String>) {
        val marker = outcomeFile.get().asFile
        marker.parentFile.mkdirs()
        val summary = if (breaking.isEmpty()) "compatible" else breaking.joinToString("\n")
        marker.writeText(summary)
    }
}
