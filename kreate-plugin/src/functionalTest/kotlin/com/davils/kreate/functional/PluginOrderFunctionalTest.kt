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
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain

class PluginOrderFunctionalTest : FunSpec({
    val workspace = tempdir()

    fun KreateBuildFixture.writeKreateFirstBuild(isExplicitApi: Boolean) {
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.davils.kreate")
                id("org.jetbrains.kotlin.jvm")
            }

            group = "com.example"

            kreate {
                platform {
                    javaVersion = JavaVersion.VERSION_${KreateBuildFixture.javaVersion}
                    explicitApi = $isExplicitApi
                    allWarningsAsErrors = false
                }
            }
            """.trimIndent()
        )
        writeKotlin("com/example/Greeter.kt", "package com.example\n\nfun greet(): String = \"hello\"\n")
    }

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        return fixture
    }

    context("Kreate applied before the Kotlin plugin") {
        test("still configures the platform once the Kotlin plugin is applied") {
            val fixture = newFixture()
            fixture.writeKreateFirstBuild(isExplicitApi = true)

            val result = fixture.buildAndFail("compileKotlin")

            result.output shouldContain "Visibility must be specified in explicit API mode"
        }

        test("compiles the same sources when explicit API mode is off") {
            val fixture = newFixture()
            fixture.writeKreateFirstBuild(isExplicitApi = false)

            val result = fixture.build("compileKotlin")

            result.output shouldContain "BUILD SUCCESSFUL"
        }
    }
})
