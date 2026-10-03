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
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

class DependencyLockingFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun KreateBuildFixture.writeLockedBuild(lockingBlock: String = "enabled = true") {
        val dependencyNothingElsePullsIn = """
            dependencies {
                implementation("org.apache.commons:commons-lang3:3.14.0")
            }
        """.trimIndent()
        writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                project {
                    name = "Sample"
                    description = "Fixture"

                    dependencyLocking {
                        $lockingBlock
                    }
                }
            """.trimIndent(),
            extra = dependencyNothingElsePullsIn
        )
    }

    fun KreateBuildFixture.lockFile(): File = file("gradle.lockfile")

    context("Dependency locking") {
        test("writes a lock file for the locked classpaths") {
            val fixture = newFixture()
            fixture.writeLockedBuild()

            val result = fixture.build("kreateResolveAndLockAll", "--write-locks")

            result.task(":kreateResolveAndLockAll")?.outcome shouldBe TaskOutcome.SUCCESS
            val lockFile = fixture.lockFile()
            lockFile.isFile shouldBe true
            lockFile.readText() shouldContain "org.apache.commons:commons-lang3:3.14.0"
            lockFile.readText() shouldContain "compileClasspath"
            lockFile.readText() shouldContain "runtimeClasspath"
        }

        test("refuses to run without --write-locks and says what to type instead") {
            val fixture = newFixture()
            fixture.writeLockedBuild()

            val result = fixture.buildAndFail("kreateResolveAndLockAll")

            result.output shouldContain "only makes sense with --write-locks"
            result.output shouldContain "./gradlew kreateResolveAndLockAll --write-locks"
            fixture.lockFile().isFile shouldBe false
        }

        test("locks only the configured classpaths") {
            val fixture = newFixture()
            fixture.writeLockedBuild(
                """
                enabled = true
                lockedClasspaths = setOf("runtimeClasspath")
                """.trimIndent()
            )

            fixture.build("kreateResolveAndLockAll", "--write-locks")

            val lockFile = fixture.lockFile()
            lockFile.readText() shouldContain "runtimeClasspath"
            lockFile.readText() shouldNotContain "compileClasspath"
        }

        test("locks the build tool classpaths too when asked to lock everything") {
            val fixture = newFixture()
            fixture.writeLockedBuild(
                """
                enabled = true
                lockAllConfigurations = true
                """.trimIndent()
            )

            fixture.build("kreateResolveAndLockAll", "--write-locks")

            fixture.lockFile().readText() shouldContain "kotlinCompilerClasspath"
        }

        test("fails a build that pulls in a dependency the lock file does not know") {
            val fixture = newFixture()
            fixture.writeLockedBuild()
            fixture.build("kreateResolveAndLockAll", "--write-locks")

            val script = fixture.file("build.gradle.kts").readText()
            val lockedDependency = """implementation("org.apache.commons:commons-lang3:3.14.0")"""
            val unlockedDependency = """implementation("org.apache.commons:commons-io:1.3.2")"""
            val withModuleTheLockDoesNotKnow = script.replace(
                lockedDependency,
                lockedDependency + "\n        " + unlockedDependency
            )
            fixture.write("build.gradle.kts", withModuleTheLockDoesNotKnow)
            val result = fixture.buildAndFail("build")

            result.output shouldContain "dependency lock state"
            result.output shouldContain "commons-io"
        }

        test("locks a classpath whose artifacts cannot be chosen between") {
            val fixture = newFixture()
            fixture.write(
                "settings.gradle.kts",
                """
                dependencyResolutionManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }

                rootProject.name = "sample"

                include(":producer")
                """.trimIndent()
            )
            fixture.write(
                "producer/build.gradle.kts",
                """
                group = "com.example"

                configurations.consumable("shared") {
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "shared"))
                    }
                    outgoing.variants.create("first") {
                        artifact(layout.projectDirectory.file("first.txt")) { type = "first" }
                    }
                    outgoing.variants.create("second") {
                        artifact(layout.projectDirectory.file("second.txt")) { type = "second" }
                    }
                }
                """.trimIndent()
            )
            fixture.write("producer/first.txt", "first")
            fixture.write("producer/second.txt", "second")

            fixture.writeLockedBuild(
                """
                enabled = true
                lockedClasspaths = setOf("consumer")
                """.trimIndent()
            )
            fixture.write(
                "build.gradle.kts",
                fixture.file("build.gradle.kts").readText() + """

                val consumerDependencies = configurations.dependencyScope("consumerDependencies")

                configurations.resolvable("consumer") {
                    extendsFrom(consumerDependencies.get())
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "shared"))
                    }
                }

                dependencies {
                    add("consumerDependencies", project(":producer"))
                }
                """.trimIndent()
            )

            val result = fixture.build("kreateResolveAndLockAll", "--write-locks")

            result.task(":kreateResolveAndLockAll")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.lockFile().readText() shouldContain "consumer"
        }

        test("registers no task while the feature is disabled") {
            val fixture = newFixture()
            fixture.writeLockedBuild("enabled = false")

            val result = fixture.build("tasks", "--all")

            result.output shouldNotContain "kreateResolveAndLockAll"
        }
    }
})
