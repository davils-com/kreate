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

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the consumer half of the local development workflow.
 *
 * Two separate checkouts, exactly as in a real workspace: one publishes, the other resolves. The
 * only thing they share is the pair of machine level locations the feature is built around, which
 * is precisely the coupling being tested.
 *
 * The assertions are all about *which version was resolved*, because that is the only observable
 * that matters and the one a plausible-looking implementation can get wrong in silence.
 */
@DisplayName("com.davils.kreate.settings")
class LocalResolutionFunctionalTest {

    @TempDir
    lateinit var producerDirectory: File

    @TempDir
    lateinit var consumerDirectory: File

    /**
     * A library that publishes `com.example:library` and can be installed locally.
     */
    private fun producer(version: String = "1.1.0"): KreateBuildFixture {
        val fixture = KreateBuildFixture(producerDirectory)
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

    /**
     * A build that depends on `com.example:library` and prints what it resolved.
     *
     * The dependency is declared at the released version throughout. Nothing in the consumer ever
     * names a snapshot — if one turns up in the output, substitution put it there.
     */
    private fun consumer(
        producer: KreateBuildFixture,
        dependency: String,
        extraRepositories: String = ""
    ): KreateBuildFixture {
        val fixture = KreateBuildFixture(consumerDirectory)
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

    @Nested
    @DisplayName("substitution")
    inner class Substitution {

        @Test
        @DisplayName("resolves the local build in place of the released version")
        fun substitutesDirectDependency() {
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            val app = consumer(library, """library("com.example:library:1.1.0")""")
            val result = app.build("printResolved")

            result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
        }

        @Test
        @DisplayName("survives a BOM that constrains the released version")
        fun survivesABomConstraint() {
            // The case that breaks the obvious implementation. Every Davils library is consumed
            // through a BOM, so the module is requested without a version and a platform supplies
            // the constraint. `useVersion` would lose the following conflict resolution, because
            // Gradle orders `1.1.0-SNAPSHOT` below `1.1.0`.
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            val app = consumer(
                library,
                """
                library(platform("com.example:library-bom:1.1.0"))
                library("com.example:library")
                """.trimIndent()
            )
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
            app.write(
                "settings.gradle.kts",
                app.file("settings.gradle.kts").readText() + "\ninclude(\"bom\")\n"
            )
            app.build(":bom:publishToMavenLocal")

            val result = app.build("printResolved")

            result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
        }

        @Test
        @DisplayName("leaves a sibling in the same group that was never published locally alone")
        fun ignoresUnpublishedCoordinates() {
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            // `com.example:sibling` shares the group but was never installed by a local publish.
            // Substitution matches exact `group:name` pairs and the injected repository is
            // filtered with `includeModule`, so this must resolve exactly as it always did — a
            // group wildcard would instead rewrite it to a version that does not exist.
            val app = consumer(
                library,
                dependency = """
                library("com.example:library:1.1.0")
                library("com.example:sibling:2.5.0")
                """.trimIndent(),
                // Unfiltered, so the sibling is reachable. The injected KreateLocal repository is
                // a second, narrower declaration and is the only one that may serve `library`.
                extraRepositories = "mavenLocal()"
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
            app.write(
                "settings.gradle.kts",
                app.file("settings.gradle.kts").readText() + "\ninclude(\"sibling\")\n"
            )
            app.build(":sibling:publishToMavenLocal")

            val result = app.build("printResolved")

            result.output shouldContain "RESOLVED com.example:library:1.1.0-SNAPSHOT"
            result.output shouldContain "RESOLVED com.example:sibling:2.5.0"
        }
    }

    @Nested
    @DisplayName("switching off")
    inner class SwitchedOff {

        @Test
        @DisplayName("resolves the released version again with -Pkreate.local=false")
        fun explicitOptOut() {
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            // The release has to exist for this to resolve at all, which is the point: with the
            // feature off, the consumer is back to ordinary resolution.
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

        @Test
        @DisplayName("does nothing at all in CI")
        fun offInCi() {
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            val app = consumer(library, """library("com.example:library:1.1.0")""")
            app.withEnvironment("GITHUB_ACTIONS", "true")

            // The state directory is populated, so the CI guard has to fail rather than quietly
            // resolve released versions: a runner carrying a developer's state is a broken runner.
            val result = app.buildAndFail("printResolved")

            result.output shouldContain "local development state while running in CI"
            result.output shouldContain "GITHUB_ACTIONS"
        }
    }

    @Nested
    @DisplayName("the banner")
    inner class Banner {

        @Test
        @DisplayName("says what is being substituted, so a surprising build explains itself")
        fun announcesLocalMode() {
            val library = producer(version = "1.1.0")
            library.build("kreateLocalPublish")

            val app = consumer(library, """library("com.example:library:1.1.0")""")
            val result = app.build("printResolved")

            result.output shouldContain "Kreate local mode is ON"
            result.output shouldContain "library"
            result.output shouldContain "1.1.0-SNAPSHOT"
            result.output shouldContain "Dependency locking is off"
        }

        @Test
        @DisplayName("stays quiet when nothing is published")
        fun quietWhenInactive() {
            val library = producer(version = "1.1.0")
            val app = consumer(library, """library("com.example:library:1.1.0")""")

            val result = app.build("printResolved")

            // Nothing was published, so nothing is substituted and the plugin must not announce
            // a mode it is not in. A banner on every build would be noise that trains people to
            // stop reading exactly the line they need when it does apply.
            result.output shouldNotContain "Kreate local mode is ON"
            result.output shouldNotContain "1.1.0-SNAPSHOT"
        }
    }
}
