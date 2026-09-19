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

package com.davils.kreate.module.project.configuration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

private const val FIXTURES = "com.davils.kreate.module.project.configuration.fixtures"
private const val SCHEMA_FIXTURES = "$FIXTURES.SchemaFixturesKt"
private const val OBJECT_HOLDER = "$FIXTURES.SchemaHolder"
private const val COMPATIBLE_EXPORT = """{"title":"server"}"""
private const val BREAKING_EXPORT = """{"title":"server","note":"breaking"}"""

/**
 * The reflective contract between Kreate and whatever configuration library a project uses.
 *
 * Kreate depends on no such library, so this is where the reading is held: which spellings of an
 * accessor are accepted, how a declaration inside an `object` is reached, that a missing facade says
 * what is missing rather than throwing out of the middle of a task, and that the kind of each change
 * is read rather than assumed.
 */
@DisplayName("Configuration schema reflection")
class SchemaReflectionTest {

    /**
     * The classpath this test itself runs on.
     *
     * The reflection deliberately builds a loader of its own parented to the platform loader, so
     * handing it the test's own classpath is the only way a fixture on it becomes visible - which is
     * also the isolation the task relies on when it reads a consumer's classes.
     */
    private fun ownClasspath(): List<File> = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .map { entry -> File(entry) }

    private fun <T> reflecting(block: (SchemaReflection) -> T): T =
        SchemaReflection(ownClasspath()).use(block)

    @Test
    @DisplayName("reads a declaration named the way a property is named")
    fun readsPropertyAccessor() {
        val exported = reflecting { reflection ->
            val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
            reflection.exportOf(schema)
        }

        exported shouldContain "server"
    }

    @Test
    @DisplayName("reads a declaration named the way a function is named")
    fun readsFunctionAccessor() {
        val exported = reflecting { reflection ->
            val schema = reflection.declarationOf(SCHEMA_FIXTURES, "gatewaySchema")
            reflection.exportOf(schema)
        }

        exported shouldContain "gateway"
    }

    @Test
    @DisplayName("reads a declaration held by a Kotlin object")
    fun readsObjectMember() {
        val exported = reflecting { reflection ->
            val schema = reflection.declarationOf(OBJECT_HOLDER, "clientSchema")
            reflection.exportOf(schema)
        }

        exported shouldContain "client"
    }

    @Test
    @DisplayName("names the accessor it looked for when there is none")
    fun reportsMissingAccessor() {
        val failure = shouldThrow<GradleException> {
            reflecting { reflection -> reflection.declarationOf(SCHEMA_FIXTURES, "absentSchema") }
        }

        failure.message.shouldContain("absentSchema")
        failure.message.shouldContain("getAbsentSchema")
    }

    @Test
    @DisplayName("names the class it looked for when it is not on the classpath")
    fun reportsMissingHolder() {
        val failure = shouldThrow<GradleException> {
            reflecting { reflection -> reflection.declarationOf("com.acme.NotHere", "schema") }
        }

        failure.message.shouldContain("com.acme.NotHere")
        failure.message.shouldContain("runtime classpath")
    }

    @Test
    @DisplayName("reports a change the library called breaking as breaking")
    fun readsBreakingKind() {
        val differences = reflecting { reflection ->
            val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
            reflection.differencesBetween(schema, BREAKING_EXPORT)
        }

        differences shouldHaveSize 1
        differences.first().kind shouldBe BREAKING_KIND
    }

    @Test
    @DisplayName("reports a change the library called compatible as something else")
    fun readsCompatibleKind() {
        val differences = reflecting { reflection ->
            val schema = reflection.declarationOf(SCHEMA_FIXTURES, "serverSchema")
            reflection.differencesBetween(schema, COMPATIBLE_EXPORT)
        }

        differences shouldHaveSize 1
        differences.first().kind shouldBe "COMPATIBLE"
    }

    @Test
    @DisplayName("flattens every report of a dry run, loadable or not")
    fun readsValidationOutcomes() {
        val outcomes = reflecting { reflection ->
            val reports = reflection.declarationOf(SCHEMA_FIXTURES, "configurationReports")
            reflection.outcomesOf(reports)
        }

        outcomes shouldHaveSize 2
        outcomes.first().isLoadable shouldBe true
        outcomes.last().isLoadable shouldBe false
        outcomes.last().problems.first().shouldContain("is required")
    }
}
