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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

/**
 * Verifies that the plugin works on every Gradle version it claims to support.
 *
 * A plugin compiled against a newer Gradle API fails at runtime, not at build time, and
 * only on the consumer's machine. Running the same build against the declared minimum is
 * the only way to catch accidental use of a newer API before publication.
 */
@DisplayName("Gradle compatibility")
class GradleCompatibilityFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @ParameterizedTest(name = "applies and builds on Gradle {0}")
    @MethodSource("supportedGradleVersions")
    fun buildsOnSupportedVersions(gradleVersion: String) {
        val fixture = KreateBuildFixture(projectDir, gradleVersion)
        fixture.writeSettings()
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            project {
                name = "Sample"
                description = "Compatibility fixture"

                buildConstant {
                    enabled = true
                    className = "SampleConstants"
                    constant("flavour", "enterprise")
                }
            }
            """.trimIndent()
        )
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )

        val result = fixture.build("build")

        result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @ParameterizedTest(name = "measures coverage on Gradle {0}")
    @MethodSource("supportedGradleVersions")
    fun coverageWorksOnSupportedVersions(gradleVersion: String) {
        // The coverage integration configures a third-party plugin whose own supported Gradle
        // range is documented only loosely. Asserting it here is what turns "Kover probably
        // works on our declared minimum" into something the build knows.
        val fixture = KreateBuildFixture(projectDir, gradleVersion)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Compatibility fixture"

                    coverage {
                        enabled = true

                        verify {
                            minLineCoverage = 0
                        }
                    }
                }
            """.trimIndent(),
            extraPlugins = listOf("""id("org.jetbrains.kotlinx.kover")""")
        )
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )

        val result = fixture.build("koverXmlReport", "koverVerify")

        result.task(":koverXmlReport")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":koverVerify")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @ParameterizedTest(name = "resolves local publications on Gradle {0}")
    @MethodSource("supportedGradleVersions")
    fun localDevelopmentWorksOnSupportedVersions(gradleVersion: String) {
        // The settings plugin uses the newest Gradle API in the whole plugin —
        // `Gradle.getLifecycle()` and `IsolatedAction`, both incubating when they were
        // introduced. Nothing else here would notice if the declared minimum did not have them:
        // the plugin compiles against the version this build uses, and the failure would be a
        // `NoSuchMethodError` on a consumer's machine.
        val producerDirectory = File(projectDir, "producer").apply { mkdirs() }
        val consumerDirectory = File(projectDir, "consumer").apply { mkdirs() }

        val producer = KreateBuildFixture(producerDirectory, gradleVersion)
        producer.writeSettings("library")
        producer.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "library"
                    version { property = "library.version" }
                    publish {
                        enabled = true
                        repositories { gitlab { enabled = true } }
                    }
                }
            """.trimIndent()
        )
        producer.write("gradle.properties", "library.version=1.0.0")
        producer.writeKotlin("com/example/Library.kt", "package com.example\n\nclass Library")
        producer.build("kreateLocalPublish")

        val consumer = KreateBuildFixture(consumerDirectory, gradleVersion)
        consumer.resolvingFrom(producer)
        consumer.write(
            "settings.gradle.kts",
            """
            plugins {
                id("com.davils.kreate.settings")
            }

            rootProject.name = "consumer"
            """.trimIndent()
        )
        consumer.write(
            "build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm")
            }

            repositories {
                mavenCentral()
            }

            val library = configurations.create("library")

            dependencies {
                library("com.example:library:1.0.0")
            }

            tasks.register("printResolved") {
                val resolved = library.incoming.resolutionResult.rootComponent.map { root ->
                    root.dependencies
                        .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                        .map { it.selected.moduleVersion?.toString() }
                }
                doLast { resolved.get().forEach { println("RESOLVED ${'$'}it") } }
            }
            """.trimIndent()
        )

        val result = consumer.build("printResolved")

        result.output.contains("RESOLVED com.example:library:1.0.0-SNAPSHOT") shouldBe true
    }

    private companion object {
        /**
         * The Gradle versions the plugin is verified against.
         *
         * The declared minimum comes from the build's own compatibility constants, so the
         * test and the published documentation cannot drift apart.
         */
        @JvmStatic
        fun supportedGradleVersions(): List<String> = listOfNotNull(
            System.getProperty("kreate.test.minGradleVersion"),
            System.getProperty("kreate.test.gradleVersion")
        ).distinct()
    }
}
