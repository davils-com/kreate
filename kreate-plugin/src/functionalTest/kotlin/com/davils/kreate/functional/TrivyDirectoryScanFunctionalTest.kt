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
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

private const val ARGUMENT_LOG: String = "arguments.log"

private const val MISCONFIGURED_REPORT: String =
    """{"Results":[{"Target":"Dockerfile","Misconfigurations":[{"ID":"DS-0002","Severity":"HIGH",""" +
        """"Title":"Image user should not be root","Resolution":"Add USER","CauseMetadata":{"StartLine":2}}]}]}"""

private const val CLEAN_REPORT: String = """{"SchemaVersion":2}"""

class TrivyDirectoryScanFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun KreateBuildFixture.installFakeTrivy(report: String): File {
        val toolDirectory = createTempDirectory(workspace.toPath(), "tool").toFile()
        val arguments = toolDirectory.resolve(ARGUMENT_LOG)
        val trivy = toolDirectory.resolve("trivy")
        trivy.writeText(
            """
            #!/bin/sh
            echo "${'$'}@" >> "${arguments.absolutePath}"
            report='$CLEAN_REPORT'
            case "${'$'}*" in
                *"--scanners misconfig"*) report='$report' ;;
            esac
            while [ ${'$'}# -gt 0 ]; do
                if [ "${'$'}1" = "--output" ]; then
                    printf '%s' "${'$'}report" > "${'$'}2"
                fi
                shift
            done
            exit 0
            """.trimIndent() + "\n"
        )
        trivy.setExecutable(true)
        withEnvironment("PATH", "${toolDirectory.absolutePath}${File.pathSeparator}${System.getenv("PATH")}")
        return arguments
    }

    fun newBuild(trivyBlock: String, extra: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            $extra

            trivy {
                enabled = true
                useInstalled = true
                secrets { runOnCheck = false }
                vulnerability { failOnMissingLockFiles = false }
                license { failOnMissingLockFiles = false }
                $trivyBlock
            }
            """.trimIndent()
        )
        return fixture
    }

    context("Trivy scans of the project directory").config(enabledOrReasonIf = requiresPosixShell) {
        test("are not there unless they are enabled") {
            val fixture = newBuild("")

            val tasks = fixture.build("tasks", "--all").output

            tasks.contains("kreateTrivyMisconfigScan") shouldBe false
            tasks.contains("kreateTrivyFilesystemScan") shouldBe false
        }

        test("a disabled scan does not run for the gate") {
            val fixture = newBuild("", "assurance { enabled = true }")
            val arguments = fixture.installFakeTrivy(CLEAN_REPORT)

            fixture.build("kreateAssurance")

            arguments.readText().contains("misconfig") shouldBe false
        }

        test("the misconfiguration scan reads the project and skips generated directories") {
            val fixture = newBuild("misconfig { enabled = true }")
            val arguments = fixture.installFakeTrivy(CLEAN_REPORT)

            val result = fixture.build("kreateTrivyMisconfigScan")

            result.task(":kreateTrivyMisconfigScan")?.outcome shouldBe TaskOutcome.SUCCESS
            val call = arguments.readText()
            call shouldContain "--scanners misconfig"
            call shouldContain "--skip-dirs build"
            call shouldContain "--skip-dirs .gradle"
            call shouldContain "--severity CRITICAL,HIGH,MEDIUM,LOW"
        }

        test("the misconfiguration scan fails on a finding, with its place") {
            val fixture = newBuild("misconfig { enabled = true }")
            fixture.installFakeTrivy(MISCONFIGURED_REPORT)

            val result = fixture.buildAndFail("kreateTrivyMisconfigScan")

            result.output shouldContain "HIGH DS-0002 Dockerfile: Image user should not be root"
            result.output shouldContain "Trivy found insecure configuration!"
        }

        test("the filesystem scan runs every scanner in one process") {
            val fixture = newBuild("filesystem { enabled = true }")
            val arguments = fixture.installFakeTrivy(CLEAN_REPORT)

            fixture.build("kreateTrivyFilesystemScan")

            arguments.readText() shouldContain "--scanners vuln,secret,misconfig"
        }

        test("the aggregate task includes what is enabled") {
            val fixture = newBuild("misconfig { enabled = true }")
            fixture.installFakeTrivy(CLEAN_REPORT)

            val result = fixture.build("kreateTrivyScan")

            result.task(":kreateTrivyMisconfigScan")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("feeds the gate with a finding document") {
            val fixture = newBuild("misconfig { enabled = true }", "assurance { enabled = true }")
            fixture.installFakeTrivy(MISCONFIGURED_REPORT)

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [FINDINGS]"
            fixture.file("build/kreate/assurance/documents/trivy-misconfig.json").shouldExist()
            fixture.file("build/kreate/assurance/reports/report.sarif").readText() shouldContain "DS-0002"
        }

        test("a scan that must not fail stays in the report without blocking the gate") {
            val fixture = newBuild(
                "misconfig { enabled = true; failOnFindings = false }",
                "assurance { enabled = true }"
            )
            fixture.installFakeTrivy(MISCONFIGURED_REPORT)

            val result = fixture.build("kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
            fixture.file("build/kreate/assurance/reports/report.json").readText() shouldContain "DS-0002"
        }
    }

    context("vulnerability options").config(enabledOrReasonIf = requiresPosixShell) {
        test("asks Trivy to leave out what has no fix") {
            val fixture = newBuild("vulnerability { ignoreUnfixed = true }")
            fixture.write("gradle.lockfile", "empty=\n")
            val arguments = fixture.installFakeTrivy(CLEAN_REPORT)

            fixture.build("kreateTrivyVulnerabilityScan")

            arguments.readText() shouldContain "--ignore-unfixed"
        }
    }

    context("a missing lock file").config(enabledOrReasonIf = requiresPosixShell) {
        test("is incomplete for the gate, not a crash and not a pass") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild(
                """
                ${KreateBuildFixture.platformBlock}

                assurance { enabled = true }

                trivy {
                    enabled = true
                    useInstalled = true
                }
                """.trimIndent()
            )
            fixture.installFakeTrivy(CLEAN_REPORT)

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }
    }
})
