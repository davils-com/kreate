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
package com.davils.kreate.assurance.rule

import com.davils.kreate.assurance.finding.parseSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

private val RULE_REFERENCE: Regex = Regex("""(?:Hit\(|to )"([A-Z][A-Z_]+)"""")

private val RULE_CONSTANT: Regex = Regex("""const val [A-Z_]+: String = "([A-Z][A-Z_]{3,})"""")

class RuleCatalogTest : FunSpec({
    context("the catalog") {
        test("explains every rule it holds") {
            RuleCatalog.rules.shouldNotBeEmpty()
            RuleCatalog.rules.forEach { rule ->
                rule.summary.isNotBlank() shouldBe true
                rule.explanation.isNotBlank() shouldBe true
            }
        }

        test("holds every rule once") {
            val ids = RuleCatalog.rules.map { rule -> rule.id }

            ids.distinct() shouldBe ids
        }

        test("rates every rule") {
            val rated = RuleCatalog.rules.map { rule -> parseSeverity(rule.severity.name) }

            rated shouldBe RuleCatalog.rules.map { rule -> rule.severity }
        }

        test("holds every rule the scanners emit") {
            val sources = File("src/main/kotlin/com/davils/kreate/inspection").walkTopDown()
            val code = sources.filter { file -> file.extension == "kt" }.joinToString("\n") { file -> file.readText() }
            val emitted = RULE_REFERENCE.findAll(code) + RULE_CONSTANT.findAll(code)
            val ids = emitted.map { match -> match.groupValues[1] }.toSet()
            val inspection = ids.filter { id -> id.contains('_') }

            inspection.filter { id -> RuleCatalog.find(id) == null }.shouldBeEmpty()
        }
    }
})
