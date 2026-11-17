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

package com.davils.kreate.detekt.wiring

import com.davils.kreate.KreateExtension
import com.davils.kreate.detekt.DetektTaskNames
import com.davils.kreate.evaluatedKreateProject
import com.davils.kreate.freshDirectory
import dev.detekt.gradle.Detekt
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.gradle.api.Project

private const val JVM_PLUGIN: String = "org.jetbrains.kotlin.jvm"

private fun Project.dependencyNamesOf(taskName: String): Set<String> {
    val task = tasks.getByName(taskName)
    val dependencies = task.taskDependencies.getDependencies(task)
    return dependencies.map { dependency -> dependency.name }.toSet()
}

private fun Project.detektPlugins(): List<String> {
    val configuration = configurations.getByName("detektPlugins")
    return configuration.dependencies.map { dependency -> "${dependency.group}:${dependency.name}" }
}

class DetektExtrasTest : FunSpec({
    val workspace = tempdir()

    fun detektProject(configure: (KreateExtension) -> Unit): Project =
        evaluatedKreateProject(workspace.freshDirectory(), JVM_PLUGIN) { kreate ->
            kreate.project.detekt { enabled.set(true) }
            configure(kreate)
        }

    context("the library rules") {
        test("are absent by default") {
            detektProject { }.detektPlugins() shouldNotContain "dev.detekt:detekt-rules-libraries"
        }

        test("join the Detekt plugins when asked for") {
            val project = detektProject { kreate -> kreate.project.detekt { libraryRules.set(true) } }

            project.detektPlugins() shouldContain "dev.detekt:detekt-rules-libraries"
        }
    }

    context("the analysis with type resolution") {
        test("is left out of check by default") {
            detektProject { }.dependencyNamesOf("check") shouldNotContain "detektMain"
        }

        test("runs on check and with kreateDetekt when asked for") {
            val project = detektProject { kreate -> kreate.project.detekt { typeResolution.set(true) } }

            project.dependencyNamesOf("check") shouldContainAll listOf("detektMain", "detektTest")
            project.dependencyNamesOf(DetektTaskNames.ANALYSE) shouldContain "detektMain"
        }
    }

    context("the formatter") {
        test("is no analysis and writes no SARIF report") {
            val project = detektProject { kreate ->
                kreate.project.detekt {
                    formatting.set(true)
                    reports { sarif { required.set(true) } }
                }
            }
            val formatter = project.tasks.getByName(DetektTaskNames.FORMAT_APPLY) as Detekt
            val analyses = project.analysisTasks().map { task -> task.name }

            formatter.reports.sarif.required.get() shouldBe false
            analyses shouldNotContain DetektTaskNames.FORMAT_APPLY
            analyses shouldContain "detektMain"
        }

        test("is never part of check") {
            val project = detektProject { kreate ->
                kreate.project.detekt {
                    formatting.set(true)
                    typeResolution.set(true)
                }
            }

            project.dependencyNamesOf("check") shouldNotContain DetektTaskNames.FORMAT_APPLY
        }
    }
})
