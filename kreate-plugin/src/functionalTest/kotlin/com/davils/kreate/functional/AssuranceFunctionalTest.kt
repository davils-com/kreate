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

private const val EVALUATED_AT: String = "2026-11-06T12:00:00Z"

private const val RESULT_FILE: String = "build/kreate/assurance/result.json"

private fun documentWith(status: String, severity: String = "HIGH"): String = """
    {"source":"external","status":"$status","evaluatedAt":"$EVALUATED_AT","findings":[
      {"ruleId":"EXAMPLE_RULE","source":"external","severity":"$severity","title":"An example","module":"com.example:lib"}
    ]}
""".trimIndent()

private fun documentOf(vararg modules: String): String {
    val findings = modules.map { module ->
        """{"ruleId":"EXAMPLE_RULE","source":"external","severity":"HIGH","title":"An example","module":"$module"}"""
    }
    val listed = findings.joinToString(",")
    return """{"source":"external","status":"COMPLETE","evaluatedAt":"$EVALUATED_AT","findings":[$listed]}"""
}

class AssuranceFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(status: String = "COMPLETE", severity: String = "HIGH", extra: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.write("findings.json", documentWith(status, severity))
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            assurance {
                enabled = true
                externalFindings.from("findings.json")
                $extra
            }
            """.trimIndent()
        )
        return fixture
    }

    context("kreateAssurance") {
        test("registers nothing while the feature is disabled") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild(KreateBuildFixture.platformBlock)

            val result = fixture.build("tasks", "--all")

            result.output.contains("kreateAssurance") shouldBe false
        }

        test("passes below the threshold") {
            val fixture = newBuild(severity = "LOW")

            val result = fixture.build("kreateAssurance")

            result.task(":kreateAssurance")?.outcome shouldBe TaskOutcome.SUCCESS
            fixture.file(RESULT_FILE).readText() shouldContain "\"exitCode\": 0"
        }

        test("fails on a finding, says so and leaves a machine readable result") {
            val fixture = newBuild()

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [FINDINGS]"
            fixture.file(RESULT_FILE).readText() shouldContain "\"exitCode\": 1"
        }

        test("writes the reports even though the verdict fails") {
            val fixture = newBuild()

            fixture.buildAndFail("kreateAssurance")

            fixture.file("build/kreate/assurance/reports/report.json").shouldExist()
            fixture.file("build/kreate/assurance/reports/report.sarif").shouldExist()
        }

        test("only reports when asked to") {
            val fixture = newBuild(extra = "reportOnly = true")

            val result = fixture.build("kreateAssurance")

            result.task(":kreateAssurance")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("never lets an incomplete check pass, not even when only reporting") {
            val fixture = newBuild(status = "INCOMPLETE", extra = "reportOnly = true")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
            fixture.file(RESULT_FILE).readText() shouldContain "\"exitCode\": 69"
        }

        test("reports a broken finding document as invalid") {
            val fixture = newBuild()
            fixture.write("findings.json", "not json")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "Kreate assurance [INVALID]"
            fixture.file(RESULT_FILE).readText() shouldContain "\"exitCode\": 65"
        }

        test("an ignore rule with its reason accepts a finding") {
            val fixture = newBuild(
                extra = """ignore("EXAMPLE_RULE") { reason = "Not reachable."; expires = "2999-01-31" }"""
            )

            val result = fixture.build("kreateAssurance")

            result.task(":kreateAssurance")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("one id can be ignored for several modules, each with a rule of its own") {
            val fixture = newBuild(
                extra = """
                    ignore("EXAMPLE_RULE") { reason = "Not reachable."; module = "com.example:lib"; expires = "2999-01-31" }
                    ignore("EXAMPLE_RULE") { reason = "Test only."; module = "com.example:other"; expires = "2999-01-31" }
                """.trimIndent()
            )
            fixture.write("findings.json", documentOf("com.example:lib", "com.example:other"))

            val result = fixture.build("kreateAssurance")

            result.task(":kreateAssurance")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        test("an ignore rule for one module leaves the same id of another module reported") {
            val fixture = newBuild(
                extra = """
                    ignore("EXAMPLE_RULE") { reason = "Not reachable."; module = "com.example:lib"; expires = "2999-01-31" }
                """.trimIndent()
            )
            fixture.write("findings.json", documentOf("com.example:lib", "com.example:other"))

            fixture.buildAndFail("kreateAssurance").output shouldContain "[FINDINGS]"
        }

        test("an expired ignore rule accepts nothing") {
            val fixture = newBuild(
                extra = """ignore("EXAMPLE_RULE") { reason = "Not reachable."; expires = "2001-01-31" }"""
            )

            fixture.buildAndFail("kreateAssurance").output shouldContain "[FINDINGS]"
        }

        test("an ignore rule without a reason is refused") {
            val fixture = newBuild(extra = """ignore("EXAMPLE_RULE") { }""")

            fixture.buildAndFail("kreateAssurance").output shouldContain "needs a reason"
        }

        test("forbids hiding a severity the build declares unignorable") {
            val fixture = newBuild(
                extra = """
                    ignore("EXAMPLE_RULE") { reason = "Not reachable." }
                    forbidIgnoreOf.add(com.davils.kreate.assurance.Severity.HIGH)
                """.trimIndent()
            )

            fixture.buildAndFail("kreateAssurance").output shouldContain "[FINDINGS]"
        }

        test("runs on check") {
            val fixture = newBuild()

            fixture.buildAndFail("check").output shouldContain "Kreate assurance [FINDINGS]"
        }

        test("stays out of check when told to") {
            val fixture = newBuild(extra = "runOnCheck = false")

            val result = fixture.build("check")

            result.task(":kreateAssurance") shouldBe null
        }

        test("a baseline accepts today's findings, and only those") {
            val fixture = newBuild()
            fixture.build("kreateAssuranceBaselineCreate")
            fixture.file("kreate-baseline.json").shouldExist()

            val accepted = fixture.build("kreateAssurance")
            accepted.task(":kreateAssurance")?.outcome shouldBe TaskOutcome.SUCCESS

            fixture.write("findings.json", documentWith("COMPLETE").replace("EXAMPLE_RULE", "NEW_RULE"))
            fixture.buildAndFail("kreateAssurance").output shouldContain "[FINDINGS]"
        }

        test("records no baseline while a source is incomplete") {
            val fixture = newBuild(status = "INCOMPLETE")

            fixture.buildAndFail("kreateAssuranceBaselineCreate").output shouldContain "[INCOMPLETE]"
        }

        test("prunes what no longer occurs") {
            val fixture = newBuild()
            fixture.build("kreateAssuranceBaselineCreate")
            fixture.write("findings.json", documentWith("COMPLETE", "LOW").replace("EXAMPLE_RULE", "OTHER"))

            fixture.build("kreateAssuranceBaselinePrune")

            val baseline = fixture.file("kreate-baseline.json")
            baseline.readText().contains("EXAMPLE_RULE") shouldBe false
        }

        test("reuses the configuration cache on a second run") {
            val fixture = newBuild(severity = "LOW")
            fixture.build("kreateAssurance")

            fixture.build("kreateAssurance").output shouldContain "Reusing configuration cache"
        }

        test("a changed finding document reruns the verdict") {
            val fixture = newBuild(severity = "LOW")
            fixture.build("kreateAssurance")
            fixture.write("findings.json", documentWith("COMPLETE"))

            fixture.buildAndFail("kreateAssurance").output shouldContain "[FINDINGS]"
        }
    }
})
