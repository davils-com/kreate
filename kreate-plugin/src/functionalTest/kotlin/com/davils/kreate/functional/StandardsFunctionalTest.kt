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

private const val REVIEWED_LOCK: String = """
{
  "schemaVersion": 1,
  "standards": [
    {
      "id": "document:ecma-404",
      "reviewedOn": "2026-11-01",
      "snapshot": { "title": "ECMA-404" }
    }
  ]
}
"""

class StandardsFunctionalTest : FunSpec({
    val workspace = kreateWorkspace()

    fun newBuild(standardsBlock: String, assuranceBlock: String = ""): KreateBuildFixture {
        val fixture = KreateBuildFixture.createIn(workspace)
        fixture.writeSettings()
        fixture.writeBuild(
            kreateBlock = """
                ${KreateBuildFixture.platformBlock}

                assurance {
                    enabled = true
                    $assuranceBlock
                    standards {
                        enabled = true
                        $standardsBlock
                    }
                }
            """.trimIndent()
        )
        val script = fixture.file("build.gradle.kts").readText()
        fixture.write("build.gradle.kts", "import java.time.Duration\n\n$script")
        return fixture
    }

    context("the standards") {
        test("are not checked unless they are enabled") {
            val fixture = KreateBuildFixture.createIn(workspace)
            fixture.writeSettings()
            fixture.writeBuild("""${KreateBuildFixture.platformBlock}

                assurance { enabled = true }
            """.trimIndent())

            fixture.build("tasks", "--all").output shouldNotContain "kreateStandards"
        }

        test("refuse a declaration nothing can check without an interval") {
            val fixture = newBuild("""din("DIN 5008")""")

            val result = fixture.buildAndFail("kreateStandardsInventory")

            result.output shouldContain "din:5008 needs reviewEvery"
        }

        test("block the gate while a declared standard was never reviewed") {
            val fixture = newBuild("""document("ECMA-404") { reviewEvery = Duration.ofDays(365) }""")

            val result = fixture.buildAndFail("kreateAssurance")

            result.output shouldContain "STANDARD_UNREVIEWED: document:ecma-404 is declared but was never reviewed"
            result.output shouldContain "Kreate assurance [FINDINGS]"
        }

        test("pass once standards.lock records the review") {
            val fixture = newBuild("""document("ECMA-404") { reviewEvery = Duration.ofDays(36500) }""")
            fixture.write("standards.lock", REVIEWED_LOCK.trimStart())

            val result = fixture.build("kreateStandardsInventory")

            result.output shouldContain "Checked 1 standard(s): 0 finding(s)."
        }

        test("name a standard the code refers to without declaring it, with its file and line") {
            val fixture = newBuild("")
            fixture.writeKotlin("Merge.kt", "package sample\n\nval specification = \"RFC 7396\"\n")

            val result = fixture.build("kreateStandardsInventory")

            result.output shouldContain "MEDIUM STANDARD_UNDECLARED: src/main/kotlin/Merge.kt names rfc:7396"
            fixture.file("build/kreate/assurance/documents/standards-inventory.json").readText() shouldContain
                "\"line\": 3"
        }

        test("let an ignore rule accept a standard the code only mentions") {
            val ignore = """ignore("STANDARD_UNDECLARED") { reason = "Only mentioned."; module = "rfc:7396" }"""
            val fixture = newBuild("", ignore)
            fixture.writeKotlin("Merge.kt", "package sample\n\nval specification = \"RFC 7396\"\n")

            val result = fixture.build("kreateAssurance")

            result.output shouldContain "Kreate assurance [PASSED]"
        }

        test("are cached while nothing they read changes") {
            val fixture = newBuild("")

            fixture.build("kreateStandardsInventory")
            val second = fixture.build("kreateStandardsInventory")

            second.task(":kreateStandardsInventory")?.outcome?.name shouldBe "UP_TO_DATE"
        }
    }
})
