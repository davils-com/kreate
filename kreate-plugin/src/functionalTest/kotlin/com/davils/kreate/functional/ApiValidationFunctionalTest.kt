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

class ApiValidationFunctionalTest : FunSpec({

    val workspace = kreateWorkspace()

    fun KreateBuildFixture.writeSource(body: String) {
        writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            $body
            """.trimIndent()
        )
    }

    fun KreateBuildFixture.writeApiValidationBuild(apiBlock: String = "enabled = true") {
        writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            project {
                name = "Sample"
                description = "Fixture"

                apiValidation {
                    $apiBlock
                }
            }
            """.trimIndent()
        )
    }

    fun KreateBuildFixture.dump(): File = file("api/sample.api")

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeSource("class Sample {\n    fun greet(): String = \"hello\"\n}")
        return fixture
    }

    context("API validation") {
        test("records the public interface in the dump") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()

            val result = fixture.build("kreateApiDump")

            result.task(":kreateApiDump")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.dump().readText() shouldContain "public final class com/example/Sample {"
            fixture.dump().readText() shouldContain "public final fun greet ()Ljava/lang/String;"
        }

        test("passes the check against a freshly written dump") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")

            val result = fixture.build("kreateApiCheck")

            result.task(":kreateApiCheck")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("fails the check and names the dump task when the interface changed") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")

            fixture.writeSource("class Sample {\n    fun greet(): String = \"hello\"\n    fun added(): Int = 1\n}")
            val result = fixture.buildAndFail("kreateApiCheck")

            result.output shouldContain "The public binary interface of project ':' changed."
            result.output shouldContain "public final fun added ()I"
            result.output shouldContain "./gradlew :kreateApiDump"
        }

        test("explains that no dump has been recorded yet") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()

            val result = fixture.buildAndFail("kreateApiCheck")

            result.output shouldContain "No binary interface dump has been recorded"
            result.output shouldContain "./gradlew :kreateApiDump"
        }

        test("runs as part of the check lifecycle task") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")

            val result = fixture.build("check")

            result.task(":kreateApiCheck")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("hides a declaration marked with a configured annotation") {
            val fixture = newFixture()
            fixture.writeKotlin(
                "com/example/Marker.kt",
                """
                package com.example

                @Retention(AnnotationRetention.BINARY)
                annotation class Hidden
                """.trimIndent()
            )
            fixture.writeSource(
                "class Sample {\n    fun greet(): String = \"hello\"\n    @Hidden fun secret(): Int = 1\n}"
            )
            fixture.writeApiValidationBuild(
                """
                enabled = true
                nonPublicMarkers = setOf("com.example.Hidden")
                """.trimIndent()
            )

            fixture.build("kreateApiDump")

            fixture.dump().readText() shouldContain "public final fun greet ()Ljava/lang/String;"
            fixture.dump().readText() shouldNotContain "secret"
        }

        test("is up to date on a second run and reuses the configuration cache") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")
            fixture.build("kreateApiCheck")

            val result = fixture.build("kreateApiCheck")

            result.task(":kreateApiCheck")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            result.output shouldContain "Configuration cache entry reused"
        }

        test("registers no tasks while the feature is disabled") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild("enabled = false")

            val result = fixture.build("tasks", "--all")

            result.output shouldNotContain "kreateApiDump"
            result.output shouldNotContain "kreateApiCheck"
        }
    }
})
