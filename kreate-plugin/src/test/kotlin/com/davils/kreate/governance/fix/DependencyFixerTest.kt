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
package com.davils.kreate.governance.fix

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val CATALOG: String = """
    [versions]
    kotlin = "2.4.21"
    jackson = "2.18.0"

    [libraries]
    kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
    kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect", version.ref = "kotlin" }
    jackson = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
    lang = "org.apache.commons:commons-lang3:3.+"
""".trimIndent()

private const val JACKSON: String = "com.fasterxml.jackson.core:jackson-databind"

class DependencyFixerTest : FunSpec({
    context("the fixer") {
        test("raises a vulnerable version to the highest fix") {
            val vulnerable = listOf(
                VulnerableVersion("com.fasterxml.jackson.core:jackson-databind", "2.18.0", "2.18.2"),
                VulnerableVersion("com.fasterxml.jackson.core:jackson-databind", "2.18.0", "2.18.6")
            )

            val result = DependencyFixer(vulnerable, emptyMap()).fix(CATALOG)

            result.changes shouldBe listOf("jackson: 2.18.0 -> 2.18.6")
            result.text shouldContain "jackson = \"2.18.6\""
        }

        test("pins a dynamic version to the resolved one") {
            val resolved = mapOf("org.apache.commons:commons-lang3" to "3.18.0")

            val result = DependencyFixer(emptyList(), resolved).fix(CATALOG)

            result.text shouldContain "lang = \"org.apache.commons:commons-lang3:3.18.0\""
        }

        test("leaves a shared version reference to a person") {
            val vulnerable = listOf(VulnerableVersion("org.jetbrains.kotlin:kotlin-stdlib", "2.4.21", "2.4.22"))

            val result = DependencyFixer(vulnerable, emptyMap()).fix(CATALOG)

            result.changes.shouldBeEmpty()
            result.skipped.single() shouldContain "kotlin-reflect"
        }

        test("changes nothing the second time") {
            val vulnerable = listOf(VulnerableVersion(JACKSON, "2.18.0", "2.18.6"))
            val fixer = DependencyFixer(vulnerable, emptyMap())

            val again = fixer.fix(fixer.fix(CATALOG).text)

            again.changes.shouldBeEmpty()
        }

        test("ignores a fix that is no exact version") {
            val vulnerable = listOf(VulnerableVersion(JACKSON, "2.18.0", "\"]\n[x"))

            val result = DependencyFixer(vulnerable, emptyMap()).fix(CATALOG)

            result.changes.shouldBeEmpty()
        }
    }
})
