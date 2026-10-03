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

private const val SCHEMA_FIXTURES = "com.davils.kreate.configuration.schema.fixtures.SchemaFixturesKt"

class ConfigValidateTest : FunSpec({
    val workspace = tempdir()

    fun validateTask(accessor: String): ConfigValidate {
        val builder = ProjectBuilder.builder()
        val directory = workspace.freshDirectory("project")
        builder.withProjectDir(directory)
        val project = builder.build()
        val task = project.tasks.register("validate", ConfigValidate::class.java).get()
        task.runtimeClasspath.from(testRunnerClasspath())
        task.reports.set(DeclaredSchema("validation", SCHEMA_FIXTURES, accessor))
        task.reportFile.set(directory.resolve("build/validation.txt"))
        return task
    }

    fun reportOf(task: ConfigValidate): String {
        val report = task.reportFile.get()
        return report.asFile.readText()
    }

    context("ConfigValidate") {
        test("reports every configuration file that loads") {
            val task = validateTask("loadableReports")

            task.execute()

            reportOf(task) shouldBe "config://server.yaml: loads"
        }

        test("fails on a file that would not load and lists its problems") {
            val task = validateTask("configurationReports")

            val failure = shouldThrow<GradleException> { task.execute() }

            failure.message shouldContain "This repository's own configuration would not load."
            failure.message shouldContain "  config://gateway.yaml\n    port: is required"
            reportOf(task) shouldBe "config://server.yaml: loads\nconfig://gateway.yaml: fails"
        }
    }
})
