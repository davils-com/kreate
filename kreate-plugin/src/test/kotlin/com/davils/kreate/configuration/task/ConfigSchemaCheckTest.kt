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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

private const val SCHEMA_FIXTURES = "com.davils.kreate.configuration.schema.fixtures.SchemaFixturesKt"
private const val DUMP_PATH = ":kreateConfigSchemaDump"

class ConfigSchemaCheckTest : FunSpec({
    val workspace = tempdir()

    fun checkTask(schemaDirectory: File): ConfigSchemaCheck {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        val task = project.tasks.register("check", ConfigSchemaCheck::class.java).get()
        task.runtimeClasspath.from(testRunnerClasspath())
        task.schemas.set(listOf(DeclaredSchema("server", SCHEMA_FIXTURES, "serverSchema")))
        task.schemaDirectory.set(schemaDirectory)
        task.dumpTaskPath.set(DUMP_PATH)
        task.outcomeFile.set(schemaDirectory.resolve("outcome/schema-check.txt"))
        return task
    }

    fun outcomeOf(task: ConfigSchemaCheck): String {
        val outcome = task.outcomeFile.get()
        return outcome.asFile.readText()
    }

    context("ConfigSchemaCheck") {
        test("records a compatible outcome when the export still matches") {
            val directory = workspace.freshDirectory("schemas")
            directory.resolve("server.json").writeText("""{"title":"server"}""")
            val task = checkTask(directory)

            task.execute()

            outcomeOf(task) shouldBe "compatible"
        }

        test("fails on a breaking change, records it and names the dump task") {
            val directory = workspace.freshDirectory("schemas")
            directory.resolve("server.json").writeText("""{"title":"server","note":"breaking"}""")
            val task = checkTask(directory)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "breaks documents already written against it"
            failure.message shouldContain "  server: server: changed"
            failure.message shouldContain "./gradlew $DUMP_PATH"
            outcomeOf(task) shouldBe "  server: server: changed"
        }

        test("fails when no export has been recorded for a declared schema") {
            val directory = workspace.freshDirectory("schemas")
            val task = checkTask(directory)

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "No export has been recorded for the configuration schema 'server'"
            failure.message shouldContain directory.resolve("server.json").path
        }
    }
})
