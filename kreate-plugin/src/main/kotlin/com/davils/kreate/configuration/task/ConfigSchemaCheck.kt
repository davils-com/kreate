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

import com.davils.kreate.configuration.schema.BREAKING_KIND
import com.davils.kreate.configuration.schema.DeclaredSchema
import com.davils.kreate.configuration.schema.SchemaDifference
import com.davils.kreate.configuration.schema.SchemaReflection
import com.davils.kreate.configuration.schema.exportFileName
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
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

private const val COMPATIBLE_SUMMARY = "compatible"

@DisableCachingByDefault(
    because = "Runs the project's own code to read a declaration, which a cached result would skip"
)
internal abstract class ConfigSchemaCheck : KreateTask(
    "Verifies that no configuration schema changed in a way that breaks a deployed document.",
    KreateTaskGroup.CONFIGURATION
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    public abstract val schemas: ListProperty<DeclaredSchema>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val expectedExports: ConfigurableFileCollection

    @get:Internal
    public abstract val schemaDirectory: DirectoryProperty

    @get:Input
    public abstract val dumpTaskPath: Property<String>

    @get:OutputFile
    public abstract val outcomeFile: RegularFileProperty

    @TaskAction
    public fun execute() {
        val directory = schemaDirectory.get().asFile
        val breaking = SchemaReflection(runtimeClasspath.files).use { reflection ->
            schemas.get().flatMap { declared -> breakingChangesOf(reflection, directory, declared) }
        }

        recordOutcome(breaking)
        if (breaking.isEmpty()) return

        throw GradleException(failureOf(breaking))
    }

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

    private fun breakingChangesOf(
        reflection: SchemaReflection,
        directory: File,
        declared: DeclaredSchema
    ): List<String> {
        val export = requireExport(directory, declared)
        val schema = reflection.declarationOf(declared.holder, declared.accessor)
        val differences = reflection.differencesBetween(schema, export.readText())
        return breakingLinesOf(declared, differences)
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
        marker.writeText(summaryOf(breaking))
    }

    private fun summaryOf(breaking: List<String>): String {
        if (breaking.isEmpty()) return COMPATIBLE_SUMMARY
        return breaking.joinToString("\n")
    }
}
