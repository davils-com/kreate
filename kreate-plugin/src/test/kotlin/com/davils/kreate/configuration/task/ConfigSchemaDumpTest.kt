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
import com.davils.kreate.freshDirectory
import com.davils.kreate.testRunnerClasspath
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val SCHEMA_FIXTURES = "com.davils.kreate.configuration.schema.fixtures.SchemaFixturesKt"

class ConfigSchemaDumpTest : FunSpec({
    val workspace = tempdir()

    fun dumpTask(schemaDirectory: File, schemas: List<DeclaredSchema>): ConfigSchemaDump {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("dump", ConfigSchemaDump::class.java).get()
        task.runtimeClasspath.from(testRunnerClasspath())
        task.schemas.set(schemas)
        task.schemaDirectory.set(schemaDirectory)
        return task
    }

    context("ConfigSchemaDump") {
        test("writes one export per declared schema, named after it") {
            val directory = workspace.freshDirectory("schemas").resolve("config-schema")
            val schemas = listOf(
                DeclaredSchema("server", SCHEMA_FIXTURES, "serverSchema"),
                DeclaredSchema("gateway", SCHEMA_FIXTURES, "gatewaySchema")
            )

            dumpTask(directory, schemas).execute()

            directory.resolve("server.json").readText() shouldContain "\"title\":\"server\""
            directory.resolve("gateway.json").readText() shouldContain "\"title\":\"gateway\""
        }

        test("creates the schema directory even when nothing is declared") {
            val directory = workspace.freshDirectory("schemas").resolve("config-schema")

            dumpTask(directory, emptyList()).execute()

            directory.shouldExist()
        }
    }
})
