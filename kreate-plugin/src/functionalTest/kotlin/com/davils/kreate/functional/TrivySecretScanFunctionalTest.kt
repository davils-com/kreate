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
import org.gradle.testkit.runner.TaskOutcome

private const val FIXTURE_PREFIX: String = "kreate-fixture-"
private const val FIXTURE_SUFFIX: String = "marker-abcdefghijklmnop"

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

class TrivySecretScanFunctionalTest : FunSpec({

    val workspace = tempdir()

    fun newFixture(): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.write("trivy-secret.yaml", FIXTURE_RULES)
        fixture.writeKotlin(
            "com/example/Sample.kt",
            """
            package com.example

            class Sample
            """.trimIndent()
        )
        return fixture
    }

    fun KreateBuildFixture.writeTrivyBuild(secretsBlock: String = "") {
        writeBuild(
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

    context("Trivy secret scan") {
        test("runs as part of check by default") {
            val fixture = newFixture()
            fixture.writeTrivyBuild()

            val result = fixture.build("check", "--dry-run")

            result.output shouldContain ":kreateTrivySecretScan SKIPPED"
        }

        test("stays out of check when runOnCheck is turned off") {
            val fixture = newFixture()
            fixture.writeTrivyBuild("runOnCheck = false")

            val result = fixture.build("check", "--dry-run")

            result.output shouldNotContain ":kreateTrivySecretScan"
        }

        test("never puts the license or vulnerability scan on check") {
            val fixture = newFixture()
            fixture.writeTrivyBuild()

            val result = fixture.build("check", "--dry-run")

            result.output shouldNotContain ":kreateTrivyLicenseScan"
            result.output shouldNotContain ":kreateTrivyVulnerabilityScan"
        }

        test("stays out of check when the Trivy module is disabled") {
            val fixture = newFixture()
            fixture.writeBuild(KreateBuildFixture.platformBlock)

            val result = fixture.build("check", "--dry-run")

            result.output shouldNotContain ":kreateTrivySecretScan"
        }

        test("names every file with a finding when it fails").config(enabledOrReasonIf = requiresTrivy) {
            val fixture = newFixture()
            fixture.writeTrivyBuild()
            fixture.write("src/main/resources/application.yaml", "fixture: $FIXTURE_PREFIX$FIXTURE_SUFFIX\n")

            val result = fixture.buildAndFail("kreateTrivySecretScan")

            result.task(":kreateTrivySecretScan")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Trivy found secrets in 1 source file(s):"
            result.output shouldContain "src/main/resources/application.yaml"
        }
    }
})
