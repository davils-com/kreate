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

private const val STRICT: String =
    """{"schemaVersion":1,"name":"acme","require":{"reportOnly":"false"},"ignoreRulesNeedExpiry":true}"""

private const val POM: String = """<project><modelVersion>4.0.0</modelVersion><groupId>org.acme</groupId>
<artifactId>policy</artifactId><version>1.0</version><packaging>json</packaging></project>"""

class PolicyFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(assuranceBlock: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            """
            ${KreateBuildFixture.platformBlock}

            assurance {
                $assuranceBlock
            }
            """.trimIndent()
        )
        return fixture
    }

    context("an organisation policy") {
        test("is not checked unless CI names it") {
            val fixture = newBuild("enabled = true")

            fixture.build("tasks", "--all").output.contains("kreateAssurancePolicyCheck") shouldBe false
        }

        test("blocks a build that weakens the gate, although the build reports only and ignores it") {
            val fixture = newBuild(
                """
                enabled = true
                reportOnly = true
                ignore("POLICY_REQUIRED_VALUE") { reason = "We know better." }
                """.trimIndent()
            )
            fixture.write("policy.json", STRICT)

            val result = fixture.buildAndFail("kreateAssurance", "-Pkreate.assurance.policy=policy.json")

            result.output shouldContain "POLICY_REQUIRED_VALUE: reportOnly is true, the policy acme requires false"
            result.output shouldContain "POLICY_IGNORE_WITHOUT_EXPIRY"
            result.output shouldContain "Kreate assurance [FINDINGS]"
        }

        test("applies to a build that switched the gate off") {
            val fixture = newBuild("enabled = false")
            fixture.write("policy.json", """{"schemaVersion":1,"name":"acme","require":{"enabled":"true"}}""")

            val result = fixture.buildAndFail("kreateAssurance", "-Pkreate.assurance.policy=policy.json")

            result.output shouldContain "POLICY_REQUIRED_VALUE: enabled is false"
        }

        test("is read from a repository as a versioned artifact") {
            val fixture = newBuild("enabled = true")
            fixture.write("local-repository/org/acme/policy/1.0/policy-1.0.json", STRICT)
            fixture.write("local-repository/org/acme/policy/1.0/policy-1.0.pom", POM)
            val script = fixture.file("build.gradle.kts").readText()
            fixture.write("build.gradle.kts", script + "\nrepositories { maven { url = uri(\"local-repository\") } }\n")

            val result = fixture.build("kreateAssurance", "-Pkreate.assurance.policy=org.acme:policy:1.0")

            result.output shouldContain "Checked the build against acme (org.acme:policy:1.0): 0 violation(s)."
        }

        test("that cannot be read leaves the gate incomplete") {
            val fixture = newBuild("enabled = true")

            val result = fixture.buildAndFail("kreateAssurance", "-Pkreate.assurance.policy=org.acme:missing:1.0")

            result.output shouldContain "Kreate assurance [INCOMPLETE]"
        }

        test("that is malformed is rejected") {
            val fixture = newBuild("enabled = true")
            fixture.write("policy.json", """{"schemaVersion":1,"name":"acme","require":{"nonsense":"true"}}""")

            val result = fixture.buildAndFail("kreateAssurance", "-Pkreate.assurance.policy=policy.json")

            result.output shouldContain "unknown options: [nonsense]"
        }

        test("starts from a template of the current settings") {
            val fixture = newBuild("enabled = true\nadvisories { enabled = true }")

            fixture.build("kreateAssurancePolicyTemplate")

            val template = fixture.file("build/kreate/assurance/policy-template.json").readText()
            template shouldContain "\"advisories.enabled\":\"true\""
            template shouldContain "\"failOn\":\"HIGH\""
        }
    }
})
