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
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for validating every target of a multiplatform project through the Kotlin plugin.
 */
@DisplayName("API validation of every multiplatform target")
class KlibApiValidationFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()
        writeCommon("class Sample {\n    fun greet(): String = \"hello\"\n}")
    }

    private fun writeCommon(body: String) {
        fixture.writeKotlin("commonMain", "com/example/Sample.kt", "package com.example\n\n$body")
    }

    private fun writeWasmOnly(body: String) {
        fixture.writeKotlin("wasmJsMain", "com/example/WasmOnly.kt", "package com.example\n\n$body")
    }

    private fun writeBuild(apiBlock: String = "enabled = true\nklib = true") {
        fixture.writeMultiplatformBuild(
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

    private val jvmDump: File get() = fixture.file("api/sample.api")
    private val klibDump: File get() = fixture.file("api/sample.klib.api")

    @Test
    @DisplayName("records the klib targets beside the JVM target")
    fun writesKlibDump() {
        writeBuild()

        fixture.build("kreateApiDump")

        jvmDump.readText() shouldContain "public final class com/example/Sample {"
        klibDump.readText() shouldContain "wasmJs"
        klibDump.readText() shouldContain "final class com.example/Sample"
    }

    @Test
    @DisplayName("passes the check against freshly written dumps")
    fun checkPassesAgainstFreshDumps() {
        writeBuild()
        fixture.build("kreateApiDump")

        val result = fixture.build("kreateApiCheck")

        result.output shouldNotContain "ABI check failed"
    }

    @Test
    @DisplayName("fails the check when only a Wasm declaration changed")
    fun checkFailsOnWasmOnlyChange() {
        writeBuild()
        fixture.build("kreateApiDump")

        writeWasmOnly("fun wasmOnly(): Int = 1")
        val result = fixture.buildAndFail("kreateApiCheck")

        result.output shouldContain "ABI check failed"
        result.output shouldContain "wasmOnly"
    }

    @Test
    @DisplayName("leaves a declaration marked non-public out of the klib dump")
    fun honoursNonPublicMarkers() {
        writeBuild("enabled = true\nklib = true\nnonPublicMarkers.add(\"com.example.Hidden\")")
        writeCommon(
            "annotation class Hidden\n\nclass Sample {\n    fun greet(): String = \"hello\"\n}\n\n" +
                "@Hidden\nclass Secret"
        )

        fixture.build("kreateApiDump")

        klibDump.readText() shouldNotContain "Secret"
        jvmDump.readText() shouldNotContain "Secret"
    }

    @Test
    @DisplayName("leaves an ignored package and its subpackages out of the klib dump")
    fun honoursIgnoredPackages() {
        writeBuild("enabled = true\nklib = true\nignoredPackages.add(\"com.example.internal\")")
        fixture.writeKotlin(
            "commonMain",
            "com/example/internal/deep/Plumbing.kt",
            "package com.example.internal.deep\n\nclass Plumbing"
        )

        fixture.build("kreateApiDump")

        klibDump.readText() shouldNotContain "Plumbing"
        klibDump.readText() shouldContain "final class com.example/Sample"
    }

    @Test
    @DisplayName("keeps the class file dump alone when the wider check is not asked for")
    fun defaultStaysOnClassFiles() {
        writeBuild("enabled = true")

        fixture.build("kreateApiDump")

        jvmDump.readText() shouldContain "public final class com/example/Sample {"
        klibDump.exists() shouldBe false
    }
}
