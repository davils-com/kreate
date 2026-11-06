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
package com.davils.kreate.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

private const val ISOLATED_PROJECTS: String = "-Dorg.gradle.unsafe.isolated-projects=true"

class IsolatedProjectsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newMultiProjectBuild(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        val settings = fixture.file("settings.gradle.kts")
        fixture.write("settings.gradle.kts", settings.readText() + "\ninclude(\"sub\")\n")
        fixture.write("trivy-secret.yaml", "rules: []\n")
        fixture.write("detekt.yaml", "")
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}
            """.trimIndent()
        )
        val rootScript = fixture.file("build.gradle.kts").readText()
        val subScript = rootScript.replace(
            KreateBuildFixture.platformBlock,
            """
            ${KreateBuildFixture.platformBlock}

            project {
                detekt {
                    enabled = true
                }
            }

            trivy {
                enabled = true
            }
            """.trimIndent()
        )
        fixture.write("sub/build.gradle.kts", subScript)
        fixture.writeKotlin("Sample.kt", "class Sample")
        return fixture
    }

    context("Isolated Projects") {
        test("a subproject reads the conventions from the root without touching its state") {
            val fixture = newMultiProjectBuild()

            val result = fixture.build(":sub:tasks", "--all", ISOLATED_PROJECTS)

            result.output shouldContain "kreateTrivySecretScan"
            result.output shouldContain "BUILD SUCCESSFUL"
        }
    }
})
