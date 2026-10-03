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
import com.davils.kreate.configuration.schema.exportFileName
import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
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

@DisableCachingByDefault(
    because = "Runs the project's own code to read a declaration, which a cached result would skip"
)
internal abstract class ConfigSchemaDump : KreateTask(
    "Writes the JSON Schema export of every declared configuration schema.",
    KreateTaskGroup.CONFIGURATION
) {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    public abstract val schemas: ListProperty<DeclaredSchema>

    @get:OutputDirectory
    public abstract val schemaDirectory: DirectoryProperty

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
