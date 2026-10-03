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

package com.davils.kreate.rules.expression

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain

class ChainedCallLimitTest : FunSpec({

    context("reports") {
        test("three chained calls") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun names(values: List<String>): List<String> =
                        values.filter { value -> value.isNotBlank() }.map { value -> value.trim() }.sorted()
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "3 calls"
        }

        test("a chain that starts with a call") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun first(): String = listOf("a").map { value -> value.uppercase() }.first()
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a chain of safe calls") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun length(value: String?): Int? = value?.trim()?.lowercase()?.length?.plus(1)
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a chain across several lines once") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun names(values: List<String>): List<String> = values
                        .filter { value -> value.isNotBlank() }
                        .map { value -> value.trim() }
                        .sorted()
                        .distinct()
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("more calls than a configured limit") {
            val rule = ChainedCallLimit(TestConfig("maxCalls" to 1))

            val findings = rule.lint(
                """
                    public fun name(value: String): String = value.trim().lowercase()
                """.trimIndent()
            )

            findings shouldHaveSize 1
        }

        test("a chain interrupted by an index access") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun first(rows: List<List<String>>): String = rows.first()[0].trim().lowercase()
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "3 calls"
        }

        test("a chain interrupted by a non-null assertion") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun name(names: Map<String, String>): String = names.get("key")!!.trim().lowercase()
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "3 calls"
        }

        test("a chain interrupted by parentheses") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun name(value: String): String = (value.trim()).lowercase().uppercase()
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "3 calls"
        }

        test("a long chain around an index access once") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun first(rows: List<List<String>>): String =
                        rows.filter { row -> row.isNotEmpty() }.map { row -> row.sorted() }.first()[0].trim()
                """.trimIndent()
            )

            findings shouldHaveSize 1
            val finding = findings.single()
            finding.message shouldContain "4 calls"
        }
    }

    context("leaves alone") {
        test("two chained calls") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun name(value: String): String = value.trim().lowercase()
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("two calls around an index access") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun first(rows: List<List<String>>): String = rows.first()[0].trim()
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("two calls around a non-null assertion and parentheses") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun name(names: Map<String, String>): String = (names.get("key"))!!.trim()
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("property access, which is no call") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun size(project: Project): Int = project.layout.buildDirectory.asFile.name.length
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("a qualified constructor call") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun file(path: String): java.io.File = java.io.File(path)
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }

        test("chains inside a lambda, which count on their own") {
            val findings = ChainedCallLimit(Config.empty).lint(
                """
                    public fun names(values: List<String>): List<String> =
                        values.map { value -> value.trim().lowercase() }.sorted()
                """.trimIndent()
            )

            findings shouldHaveSize 0
        }
    }
})
