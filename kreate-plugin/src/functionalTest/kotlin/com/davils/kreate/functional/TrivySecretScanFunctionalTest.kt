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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * The marker the fixture's own rule matches, split so that no file in this repository contains it
 * whole and Kreate's own scan has nothing to report about this test.
 */
private const val FIXTURE_PREFIX: String = "kreate-fixture-"
private const val FIXTURE_SUFFIX: String = "marker-abcdefghijklmnop"

/**
 * A secret configuration with one custom rule for the fixture's marker, so the test needs no
 * real-looking credential.
 */
private val FIXTURE_RULES: String = """
    rules:
      - id: kreate-fixture-marker
        category: general
        title: Kreate fixture marker
        severity: HIGH
        regex: 'kreate-fixture-marker-[a-z]{16}'
        keywords:
          - kreate-fixture-marker
""".trimIndent()

/**
 * Tests for how the Trivy secret scan is wired into a build.
 *
 * The wiring tests use `--dry-run`, which lists what `check` would run without running it, so they
 * need no Trivy binary. The one test that runs the scan is gated on Trivy being installed.
 */
@DisplayName("Trivy secret scan")
class TrivySecretScanFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var fixture: KreateBuildFixture

    @BeforeEach
    fun setUp() {
        fixture = KreateBuildFixture(projectDir)
        fixture.writeSettings()
        fixture.write("trivy-secret.yaml", FIXTURE_RULES)
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
    }

    private fun writeBuild(secretsBlock: String = "") {
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            trivy {
                enabled = true

                secrets {
                    $secretsBlock
                }
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("runs as part of check by default")
    fun runsOnCheckByDefault() {
        writeBuild()

        val result = fixture.build("check", "--dry-run")

        result.output shouldContain ":kreateTrivySecretScan SKIPPED"
    }

    @Test
    @DisplayName("stays out of check when runOnCheck is turned off")
    fun staysOutOfCheckWhenTurnedOff() {
        writeBuild("runOnCheck = false")

        val result = fixture.build("check", "--dry-run")

        result.output shouldNotContain ":kreateTrivySecretScan"
    }

    @Test
    @DisplayName("never puts the license or vulnerability scan on check")
    fun keepsDatabaseScansOffCheck() {
        writeBuild()

        val result = fixture.build("check", "--dry-run")

        result.output shouldNotContain ":kreateTrivyLicenseScan"
        result.output shouldNotContain ":kreateTrivyVulnerabilityScan"
    }

    @Test
    @DisplayName("stays out of check when the Trivy module is disabled")
    fun staysOutOfCheckWhenTrivyDisabled() {
        fixture.writeBuild(KreateBuildFixture.platformBlock)

        val result = fixture.build("check", "--dry-run")

        result.output shouldNotContain ":kreateTrivySecretScan"
    }

    @Test
    @EnabledIfTrivyAvailable
    @DisplayName("names every file with a finding when it fails")
    fun namesFilesWithFindings() {
        writeBuild()
        fixture.write("src/main/resources/application.yaml", "fixture: $FIXTURE_PREFIX$FIXTURE_SUFFIX\n")

        val result = fixture.buildAndFail("kreateTrivySecretScan")

        result.task(":kreateTrivySecretScan")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain "Trivy found secrets in 1 source file(s):"
        result.output shouldContain "src/main/resources/application.yaml"
    }
}
