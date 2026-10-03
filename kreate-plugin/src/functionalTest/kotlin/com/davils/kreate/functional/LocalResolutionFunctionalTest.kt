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
import io.kotest.matchers.string.shouldNotContain

class LocalResolutionFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun producer(version: String = "1.1.0"): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings("library")
        fixture.writeBuild(
            kreateBlock = """
                project {
                    name = "library"
                    version { property = "library.version" }
                    publish {
                        enabled = true
                        repositories { gitlab { enabled = true } }
                    }
                }
                ${KreateBuildFixture.platformBlock}
            """.trimIndent()
        )
        fixture.write("gradle.properties", "library.version=$version")
        fixture.writeKotlin("Library.kt", "class Library")
        return fixture
    }

    fun consumer(
        producer: KreateBuildFixture,
        dependency: String,
        extraRepositories: String = ""
    ): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.resolvingFrom(producer)

        fixture.write(
            "settings.gradle.kts",
            """
            plugins {
                id("com.davils.kreate.settings")
            }

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }

            rootProject.name = "consumer"
            """.trimIndent()
        )

        fixture.write(
            "build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm")
            }

            repositories {
                mavenCentral()
                $extraRepositories
            }

            val library = configurations.create("library")

            dependencies {
                $dependency
            }

            tasks.register("printResolved") {
                val resolved = library.incoming.resolutionResult.rootComponent.map { root ->
                    root.dependencies
                        .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                        .map { it.selected.moduleVersion?.toString() }
                        .sortedBy { it }
                }
                doLast { resolved.get().forEach { println("RESOLVED ${'$'}it") } }
            }
            """.trimIndent()
        )
        return fixture
    }

    fun KreateBuildFixture.includeInSettings(projectPath: String) {
        val settings = file("settings.gradle.kts").readText()
        write("settings.gradle.kts", settings + "\ninclude(\"$projectPath\")\n")
    }

    context("com.davils.kreate.settings") {
        context("substitution") {
            test("resolves the local build in place of the released version") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                val app = consumer(library, """library("com.example:library:1.1.0")""")
                val result = app.build("printResolved")

                result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
            }

            test("survives a BOM that constrains the released version") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                val versionlessRequestConstrainedByTheBom = """
                    library(platform("com.example:library-bom:1.1.0"))
                    library("com.example:library")
                """.trimIndent()
                val app = consumer(library, versionlessRequestConstrainedByTheBom)
                app.write(
                    "bom/build.gradle.kts",
                    """
                    plugins {
                        `java-platform`
                        `maven-publish`
                    }

                    group = "com.example"
                    version = "1.1.0"

                    dependencies {
                        constraints {
                            api("com.example:library:1.1.0")
                        }
                    }

                    publishing {
                        publications {
                            create<MavenPublication>("bom") {
                                from(components["javaPlatform"])
                                artifactId = "library-bom"
                            }
                        }
                    }
                    """.trimIndent()
                )
                app.includeInSettings("bom")
                app.build(":bom:publishToMavenLocal")

                val result = app.build("printResolved")

                result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
            }

            test("leaves a sibling in the same group that was never published locally alone") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                val unfilteredSoTheSiblingIsReachable = "mavenLocal()"
                val app = consumer(
                    library,
                    dependency = """
                    library("com.example:library:1.1.0")
                    library("com.example:sibling:2.5.0")
                    """.trimIndent(),
                    extraRepositories = unfilteredSoTheSiblingIsReachable
                )
                app.write(
                    "sibling/build.gradle.kts",
                    """
                    plugins {
                        `java-library`
                        `maven-publish`
                    }

                    group = "com.example"
                    version = "2.5.0"

                    publishing {
                        publications {
                            create<MavenPublication>("sibling") {
                                from(components["java"])
                                artifactId = "sibling"
                            }
                        }
                    }
                    """.trimIndent()
                )
                app.includeInSettings("sibling")
                app.build(":sibling:publishToMavenLocal")

                val result = app.build("printResolved")

                result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
                result.output shouldContain "RESOLVED com.example:sibling:2.5.0"
            }
        }

        context("switching off") {
            test("resolves the released version again with -Pkreate.local=false") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                library.build("publishToMavenLocal", "-Pkreate.local=false")

                val app = consumer(
                    library,
                    dependency = """library("com.example:library:1.1.0")""",
                    extraRepositories = "mavenLocal()"
                )

                val result = app.build("printResolved", "-Pkreate.local=false")

                result.output shouldContain "RESOLVED com.example:library:1.1.0"
                result.output shouldNotContain "1.1.0-SNAPSHOT"
            }

            test("does nothing at all in CI") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                val app = consumer(library, """library("com.example:library:1.1.0")""")
                app.withEnvironment("GITHUB_ACTIONS", "true")

                val result = app.buildAndFail("printResolved")

                result.output shouldContain "local development state while running in CI"
                result.output shouldContain "GITHUB_ACTIONS"
            }
        }

        context("the banner") {
            test("says what is being substituted, so a surprising build explains itself") {
                val library = producer(version = "1.1.0")
                library.build("kreateLocalPublish")

                val app = consumer(library, """library("com.example:library:1.1.0")""")
                val result = app.build("printResolved")

                result.output shouldContain "Kreate local mode is ON"
                result.output shouldContain "library"
                result.output shouldContain "1.1.0-SNAPSHOT"
                result.output shouldContain "Dependency locking is off"
            }

            test("stays quiet when nothing is published") {
                val library = producer(version = "1.1.0")
                val app = consumer(library, """library("com.example:library:1.1.0")""")

                val result = app.build("printResolved")

                result.output shouldNotContain "Kreate local mode is ON"
                result.output shouldNotContain "1.1.0-SNAPSHOT"
            }
        }
    }
})
