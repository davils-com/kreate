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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests that the settings plugin reaches a build that never applies the project plugin.
 *
 * ### Why this test exists
 *
 * A Davils repository resolves the Kreate plugin marker as an ordinary `implementation`
 * dependency of its `build-logic` included build, and `build-logic` never applies
 * `com.davils.kreate`. Anything installed by the project plugin is therefore unreachable from
 * exactly the place that decides which Kreate the repository compiles against — which is why the
 * feature needs a settings plugin at all. This suite is what makes that claim a fact rather than
 * an argument.
 *
 * The second case is the one that hurts in practice: `build-logic` calls
 * `dependencyLocking { lockAllConfigurations() }` and has a committed `gradle.lockfile` pinning
 * the released version. A local snapshot cannot be in that file. The build has to resolve the
 * snapshot anyway, and the file has to come out byte-identical.
 *
 * ### What this suite does *not* prove
 *
 * That `DeactivateLockingAction` is what produces that outcome. Gradle 9.6 was not observed to
 * enforce lock state against a selector that dependency substitution had already rewritten, so
 * the snapshot resolves here with the action registered through `afterProject`, through
 * `beforeProject`, with `force` removed, and with the action removed altogether.
 *
 * The deactivation stays regardless, because it is right on its own terms: a lock file records
 * released versions and cannot contain a machine-local snapshot, so enforcing it against one
 * would be enforcing a constraint that can never be satisfied. It is a precaution whose absence
 * this suite would not catch, and saying so here is cheaper than someone rediscovering it.
 */
@DisplayName("com.davils.kreate.settings in an included build")
class BuildLogicLocalFunctionalTest {

    @TempDir
    lateinit var producerDirectory: File

    @TempDir
    lateinit var consumerDirectory: File

    private fun producer(): KreateBuildFixture {
        val fixture = KreateBuildFixture(producerDirectory)
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

    /**
     * Writes a consumer whose `build-logic` included build resolves the producer, locks all of its
     * configurations, and never applies the Kreate project plugin.
     *
     * @param producer The fixture whose publication `build-logic` depends on.
     * @return The consumer fixture.
     */
    private fun consumerWithBuildLogic(producer: KreateBuildFixture): KreateBuildFixture {
        val fixture = KreateBuildFixture(consumerDirectory)
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

        fixture.write(
            "build-logic/build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm")
            }

            repositories {
                mavenCentral()
                // Stands in for the GitLab package registry: where the released version comes
                // from when local mode is off. The injected DavilsLocal repository is a second,
                // narrower declaration that only ever serves locally published coordinates.
                mavenLocal()
            }

            // The shape every Davils repository has, and the reason a project plugin cannot do
            // this job: `build-logic` compiles the conventions and never applies Kreate itself.
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
        return fixture
    }

    @Test
    @DisplayName("substitutes into build-logic, which never applies the project plugin")
    fun reachesBuildLogic() {
        val conventions = producer()
        conventions.build("kreateLocalPublish")

        val app = consumerWithBuildLogic(conventions)
        val result = app.build("-p", "build-logic", "printResolved")

        result.output shouldContain "RESOLVED com.example:conventions:1.0.0-SNAPSHOT"
    }

    @Test
    @DisplayName("deactivates locking that the included build's own script switched on")
    fun deactivationWinsOverTheScript() {
        val conventions = producer()

        // The real sequence: a release exists, a lock file pins it, and only then does someone
        // publish locally. Without a committed lock file the test proves nothing, because
        // locking a configuration that has no lock state does not constrain anything.
        conventions.build("publishToMavenLocal", "-Pdavils.local=false")

        val app = consumerWithBuildLogic(conventions)
        app.build("-p", "build-logic", "printResolved", "--write-locks")

        val lockFile = app.file("build-logic/gradle.lockfile")
        lockFile.readText() shouldContain "com.example:conventions:1.0.0="
        val pinned = lockFile.readText()

        conventions.build("kreateLocalPublish")
        val result = app.build("-p", "build-logic", "printResolved")

        // A lock file pinning `1.0.0` is present and the script locks every configuration, yet
        // the local snapshot resolves. This is the outcome the feature promises; it is not proof
        // that the deactivation caused it — see the class comment.
        result.output shouldContain "RESOLVED com.example:conventions:1.0.0-SNAPSHOT"

        // And the committed lock file is left exactly as it was. This is the guarantee the
        // feature makes: local mode never reads or writes lock state.
        lockFile.readText() shouldBe pinned
    }
}
