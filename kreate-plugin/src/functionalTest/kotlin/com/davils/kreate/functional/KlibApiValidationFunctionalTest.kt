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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class KlibApiValidationFunctionalTest : FunSpec({

    val workspace = tempdir()

    fun KreateBuildFixture.writeCommon(body: String) {
        writeKotlin("commonMain", "com/example/Sample.kt", "package com.example\n\n$body")
    }

    fun KreateBuildFixture.writeWasmOnly(body: String) {
        writeKotlin("wasmJsMain", "com/example/WasmOnly.kt", "package com.example\n\n$body")
    }

    fun KreateBuildFixture.writeApiValidationBuild(apiBlock: String = "enabled = true\nklib = true") {
        writeMultiplatformBuild(
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

    fun KreateBuildFixture.jvmDump(): File = file("api/sample.api")

    fun KreateBuildFixture.klibDump(): File = file("api/sample.klib.api")

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeCommon("class Sample {\n    fun greet(): String = \"hello\"\n}")
        return fixture
    }

    context("API validation of every multiplatform target") {
        test("records the klib targets beside the JVM target") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()

            fixture.build("kreateApiDump")

            fixture.jvmDump().readText() shouldContain "public final class com/example/Sample {"
            fixture.klibDump().readText() shouldContain "wasmJs"
            fixture.klibDump().readText() shouldContain "final class com.example/Sample"
        }

        test("passes the check against freshly written dumps") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")

            val result = fixture.build("kreateApiCheck")

            result.output shouldNotContain "ABI check failed"
        }

        test("fails the check when only a Wasm declaration changed") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild()
            fixture.build("kreateApiDump")

            fixture.writeWasmOnly("fun wasmOnly(): Int = 1")
            val result = fixture.buildAndFail("kreateApiCheck")

            result.output shouldContain "ABI check failed"
            result.output shouldContain "wasmOnly"
        }

        test("leaves a declaration marked non-public out of the klib dump") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild(
                "enabled = true\nklib = true\nnonPublicMarkers.add(\"com.example.Hidden\")"
            )
            fixture.writeCommon(
                """
                annotation class Hidden

                class Sample {
                    fun greet(): String = "hello"
                }

                @Hidden
                class Secret
                """.trimIndent()
            )

            fixture.build("kreateApiDump")

            fixture.klibDump().readText() shouldNotContain "Secret"
            fixture.jvmDump().readText() shouldNotContain "Secret"
        }

        test("leaves an ignored package and its subpackages out of the klib dump") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild(
                "enabled = true\nklib = true\nignoredPackages.add(\"com.example.internal\")"
            )
            fixture.writeKotlin(
                "commonMain",
                "com/example/internal/deep/Plumbing.kt",
                "package com.example.internal.deep\n\nclass Plumbing"
            )

            fixture.build("kreateApiDump")

            fixture.klibDump().readText() shouldNotContain "Plumbing"
            fixture.klibDump().readText() shouldContain "final class com.example/Sample"
        }

        test("keeps the class file dump alone when the wider check is not asked for") {
            val fixture = newFixture()
            fixture.writeApiValidationBuild("enabled = true")

            fixture.build("kreateApiDump")

            fixture.jvmDump().readText() shouldContain "public final class com/example/Sample {"
            fixture.klibDump().exists() shouldBe false
        }
    }
})
