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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BuildLogicLocalFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun producer(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings("conventions")
        fixture.writeBuild(
            kreateBlock = """
                project {
                    name = "conventions"
                    version { property = "conventions.version" }
                    publish {
                        enabled = true
                        repositories { gitlab { enabled = true } }
                    }
                }
                ${KreateBuildFixture.platformBlock}
            """.trimIndent()
        )
        fixture.write("gradle.properties", "conventions.version=1.0.0")
        fixture.writeKotlin("Conventions.kt", "class Conventions")
        return fixture
    }

    fun KreateBuildFixture.writeConventionsBuild() {
        write(
            "build-logic/build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm")
            }

            repositories {
                mavenCentral()
                // Stands in for the GitLab package registry: where the released version comes
                // from when local mode is off. The injected KreateLocal repository is a second,
                // narrower declaration that only ever serves locally published coordinates.
                mavenLocal()
            }

            // The shape a conventions build normally has, and the reason a project plugin
            // cannot do this job: `build-logic` compiles the conventions and never applies
            // Kreate itself.
            dependencyLocking {
                lockAllConfigurations()
            }

            val conventions = configurations.create("conventions")

            dependencies {
                conventions("com.example:conventions:1.0.0")
            }

            tasks.register("printResolved") {
                val resolved = conventions.incoming.resolutionResult.rootComponent.map { root ->
                    root.dependencies
                        .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                        .map { it.selected.moduleVersion?.toString() }
                }
                doLast { resolved.get().forEach { println("RESOLVED ${'$'}it") } }
            }
            """.trimIndent()
        )
    }

    fun consumerWithBuildLogic(producer: KreateBuildFixture): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.resolvingFrom(producer)

        fixture.write(
            "settings.gradle.kts",
            """
            rootProject.name = "consumer"

            includeBuild("build-logic")
            """.trimIndent()
        )
        fixture.write("build.gradle.kts", "")

        fixture.write(
            "build-logic/settings.gradle.kts",
            """
            plugins {
                id("com.davils.kreate.settings")
            }

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }

            rootProject.name = "build-logic"
            """.trimIndent()
        )

        fixture.writeConventionsBuild()
        return fixture
    }

    context("com.davils.kreate.settings in an included build") {
        test("substitutes into build-logic, which never applies the project plugin") {
            val conventions = producer()
            conventions.build("kreateLocalPublish")

            val app = consumerWithBuildLogic(conventions)
            val result = app.build("-p", "build-logic", "printResolved")

            result.output shouldContain "RESOLVED com.example:conventions:1.0.0-SNAPSHOT"
        }

        test("deactivates locking that the included build's own script switched on") {
            val conventions = producer()

            conventions.build("publishToMavenLocal", "-Pkreate.local=false")

            val app = consumerWithBuildLogic(conventions)
            app.build("-p", "build-logic", "printResolved", "--write-locks")

            val committedLockFile = app.file("build-logic/gradle.lockfile")
            committedLockFile.readText() shouldContain "com.example:conventions:1.0.0="
            val pinned = committedLockFile.readText()

            conventions.build("kreateLocalPublish")
            val result = app.build("-p", "build-logic", "printResolved")

            result.output shouldContain "RESOLVED com.example:conventions:1.0.0-SNAPSHOT"
            committedLockFile.readText() shouldBe pinned
        }
    }
})
