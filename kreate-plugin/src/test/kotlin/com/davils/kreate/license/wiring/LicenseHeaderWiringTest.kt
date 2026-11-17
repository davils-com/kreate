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

package com.davils.kreate.license.wiring

import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.freshDirectory
import com.davils.kreate.license.LicenseHeaderTaskNames
import com.davils.kreate.license.task.LicenseHeaderCheck
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.gradle.api.Project

private const val JVM_PLUGIN: String = "org.jetbrains.kotlin.jvm"

private fun Project.checkDependencies(): List<String> {
    val check = tasks.getByName("check")
    val dependencies = check.taskDependencies.getDependencies(check)
    return dependencies.map { task -> task.name }
}

class LicenseHeaderWiringTest : FunSpec({
    val workspace = tempdir()

    context("the license header check") {
        test("is not registered by default") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { }

            project.tasks.findByName(LicenseHeaderTaskNames.CHECK).shouldBeNull()
        }

        test("checks the selected files of the project on check") {
            val directory = workspace.freshDirectory()
            directory.resolve("src/main/kotlin").mkdirs()
            directory.resolve("src/main/kotlin/A.kt").writeText("class A\n")
            directory.resolve("build/generated").mkdirs()
            directory.resolve("build/generated/B.kt").writeText("class B\n")
            directory.resolve("notes.txt").writeText("notes\n")
            val project = evaluatedKreateProject(directory, JVM_PLUGIN) { kreate ->
                kreate.project.licenseHeader {
                    enabled.set(true)
                    apacheLicense("Copyright 2026 Example")
                }
            }
            val check = project.tasks.getByName(LicenseHeaderTaskNames.CHECK) as LicenseHeaderCheck
            val names = check.sources.files.map { file -> file.name }

            names shouldContain "A.kt"
            names shouldNotContain "B.kt"
            names shouldNotContain "notes.txt"
            check.notice.get() shouldStartWith "Copyright 2026 Example"
            project.checkDependencies() shouldContain LicenseHeaderTaskNames.CHECK
        }

        test("stays out of check when asked to") {
            val project = evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                kreate.project.licenseHeader {
                    enabled.set(true)
                    notice.set("Copyright 2026 Example")
                    runOnCheck.set(false)
                }
            }

            project.checkDependencies() shouldNotContain LicenseHeaderTaskNames.CHECK
        }

        test("needs a notice") {
            val failure = shouldThrow<Exception> {
                evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
                    kreate.project.licenseHeader { enabled.set(true) }
                }
            }
            val rootCause = generateSequence<Throwable>(failure) { cause -> cause.cause }.last()

            rootCause.message.orEmpty() shouldContain "has no notice"
        }
    }
})
