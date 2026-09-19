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
import com.davils.kreate.module.project.configuration.exportFileName
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes each declared configuration schema to a checked-in JSON Schema export.
 *
 * The export is the record a reviewer reads. A field made required, a type narrowed or an enum
 * constant removed is invisible in the source diff that caused it and obvious in a diff of this,
 * which is the same reason the `.api` dump exists for a published signature.
 *
 * @since 3.1.0
 */
@DisableCachingByDefault(
    because = "Runs the project's own code to read a declaration, which a cached result would skip"
)
public abstract class ConfigSchemaDump : Task(
    "Writes the JSON Schema export of every declared configuration schema.",
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
     * The directory the exports are written to.
     *
     * @since 3.1.0
     */
    @get:OutputDirectory
    public abstract val schemaDirectory: DirectoryProperty

    /**
     * Exports every declared schema.
     *
     * @return Unit
     * @since 3.1.0
     */
    @TaskAction
    public fun execute() {
        val directory = schemaDirectory.get().asFile
        directory.mkdirs()

        SchemaReflection(runtimeClasspath.files).use { reflection ->
            schemas.get().forEach { declared ->
                val schema = reflection.declarationOf(declared.holder, declared.accessor)
                val exported = reflection.exportOf(schema)
                directory.resolve(exportFileName(declared.name)).writeText(exported)
            }
        }
    }
}
