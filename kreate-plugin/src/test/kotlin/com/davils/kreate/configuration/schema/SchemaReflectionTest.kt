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

package com.davils.kreate.configuration.schema

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

private const val FIXTURES = "com.davils.kreate.configuration.schema.fixtures"
private const val SCHEMA_FIXTURES = "$FIXTURES.SchemaFixturesKt"
private const val OBJECT_HOLDER = "$FIXTURES.SchemaHolder"
private const val PLAIN_HOLDER = "$FIXTURES.PlainHolder"
private const val COMPATIBLE_EXPORT = """{"title":"server"}"""
private const val FAILING_SCHEMA_MESSAGE = "the schema could not be built"
private const val BREAKING_EXPORT = """{"title":"server","note":"breaking"}"""

private fun testRunnerClasspath(): List<File> {
    val classpath = System.getProperty("java.class.path")
    val entries = classpath.split(File.pathSeparator)
    return entries.map { entry -> File(entry) }
}

private fun <T> reflecting(block: (SchemaReflection) -> T): T =
    SchemaReflection(testRunnerClasspath()).use(block)

class SchemaReflectionTest : FunSpec({
    context("Configuration schema reflection") {
        test("reads a declaration named the way a property is named") {
            val exported = reflecting { reflection ->
                val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
                reflection.exportOf(schema)
            }

            exported shouldContain "server"
        }

        test("reads a declaration named the way a function is named") {
            val exported = reflecting { reflection ->
                val schema = reflection.declarationOf(SCHEMA_FIXTURES, "gatewaySchema")
                reflection.exportOf(schema)
            }

            exported shouldContain "gateway"
        }

        test("reads a declaration held by a Kotlin object") {
            val exported = reflecting { reflection ->
                val schema = reflection.declarationOf(OBJECT_HOLDER, "clientSchema")
                reflection.exportOf(schema)
            }

            exported shouldContain "client"
        }

        test("names the accessor it looked for when there is none") {
            val failure = shouldThrow<IllegalArgumentException> {
                reflecting { reflection -> reflection.declarationOf(SCHEMA_FIXTURES, "absentSchema") }
            }

            failure.message.shouldContain("absentSchema")
            failure.message.shouldContain("getAbsentSchema")
        }

        test("names the class it looked for when it is not on the classpath") {
            val failure = shouldThrow<IllegalArgumentException> {
                reflecting { reflection -> reflection.declarationOf("com.acme.NotHere", "schema") }
            }

            failure.message.shouldContain("com.acme.NotHere")
            failure.message.shouldContain("runtime classpath")
        }

        test("refuses a declaration that answers with nothing") {
            val failure = shouldThrow<IllegalStateException> {
                reflecting { reflection -> reflection.declarationOf(SCHEMA_FIXTURES, "unsetSchema") }
            }

            failure.message.shouldContain("answered with nothing to export")
        }

        test("refuses a holder that is neither a top level file nor a Kotlin object") {
            val failure = shouldThrow<IllegalArgumentException> {
                reflecting { reflection -> reflection.declarationOf(PLAIN_HOLDER, "schema") }
            }

            failure.message.shouldContain("has no instance to read the declaration from")
        }

        test("refuses a validation accessor that hands back something other than a list") {
            val failure = shouldThrow<IllegalArgumentException> {
                reflecting { reflection ->
                    val report = reflection.declarationOf(SCHEMA_FIXTURES, "singleReport")
                    reflection.outcomesOf(report)
                }
            }

            failure.message.shouldContain("hands back a list of reports")
        }

        test("surfaces the exception an accessor throws as the cause, naming the accessor") {
            val failure = shouldThrow<IllegalStateException> {
                reflecting { reflection -> reflection.declarationOf(SCHEMA_FIXTURES, "failingSchema") }
            }

            failure.message.shouldContain("$SCHEMA_FIXTURES.failingSchema")
            failure.message.shouldContain(FAILING_SCHEMA_MESSAGE)
            failure.cause.shouldBeInstanceOf<IllegalStateException>()
            failure.cause?.message shouldBe FAILING_SCHEMA_MESSAGE
        }

        test("refuses a report whose loadability is not a Boolean") {
            val failure = shouldThrow<IllegalStateException> {
                reflecting { reflection ->
                    val reports = reflection.declarationOf(SCHEMA_FIXTURES, "mistypedReports")
                    reflection.outcomesOf(reports)
                }
            }

            failure.message.shouldContain("$FIXTURES.MistypedReport.isLoadable")
            failure.message.shouldContain("not a Boolean")
        }

        test("reports a change the library called breaking as breaking") {
            val differences = reflecting { reflection ->
                val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
                reflection.differencesBetween(schema, BREAKING_EXPORT)
            }

            differences shouldHaveSize 1
            differences.first().kind shouldBe BREAKING_KIND
        }

        test("reports a change the library called compatible as something else") {
            val differences = reflecting { reflection ->
                val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
                reflection.differencesBetween(schema, COMPATIBLE_EXPORT)
            }

            differences shouldHaveSize 1
            differences.first().kind shouldBe "COMPATIBLE"
        }

        test("flattens every report of a dry run, loadable or not") {
            val outcomes = reflecting { reflection ->
                val reports = reflection.declarationOf(SCHEMA_FIXTURES, "configurationReports")
                reflection.outcomesOf(reports)
            }

            outcomes shouldHaveSize 2
            outcomes.first().isLoadable shouldBe true
            outcomes.last().isLoadable shouldBe false
            val unloadableProblems = outcomes.last().problems
            unloadableProblems.first() shouldContain "is required"
        }
    }
})
