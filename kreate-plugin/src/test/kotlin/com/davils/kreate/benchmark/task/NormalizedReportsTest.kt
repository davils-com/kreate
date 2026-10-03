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

package com.davils.kreate.benchmark.task

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class NormalizedReportsTest : FunSpec({
    val workspace = tempdir()

    fun reportsIn(directory: File): ConfigurableFileCollection {
        val builder = ProjectBuilder.builder()
        builder.withProjectDir(workspace.freshDirectory("project"))
        val project = builder.build()
        return project.objects.fileCollection().from(directory)
    }

    context("firstNormalizedReport") {
        test("picks the JSON report that sorts first by name") {
            val directory = workspace.freshDirectory("reports")
            directory.resolve("b-main.json").writeText("[]")
            directory.resolve("a-main.json").writeText("[]")
            directory.resolve("0-notes.txt").writeText("")

            val report = reportsIn(directory).firstNormalizedReport("main")

            report.name shouldBe "a-main.json"
        }

        test("finds a report nested below the report directory") {
            val directory = workspace.freshDirectory("reports")
            val nested = directory.resolve("main/2026")
            nested.mkdirs()
            nested.resolve("main.json").writeText("[]")

            reportsIn(directory).firstNormalizedReport("main") shouldBe nested.resolve("main.json")
        }

        test("asks for a benchmark run when no report exists for the profile") {
            val directory = workspace.freshDirectory("reports")
            val reports = reportsIn(directory)

            val failure = shouldThrow<GradleException> { reports.firstNormalizedReport("smoke") }

            failure.message shouldContain "No normalized benchmark report is available for profile 'smoke'."
            failure.message shouldContain "./gradlew benchmark"
        }
    }
})
