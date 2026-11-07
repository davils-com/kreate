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
package com.davils.kreate.api.semver

import com.davils.kreate.vcs.SemanticVersion
import com.davils.kreate.vcs.VersionBump
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val BEFORE: String = """
    public final class com/example/Sample {
    	public fun <init> ()V
    	public final fun greet ()Ljava/lang/String;
    }

    public abstract interface class com/example/Shape {
    	public abstract fun area ()D
    }

    public class com/example/Base : com/example/Shape {
    	public fun <init> ()V
    	public fun area ()D
    }

""".trimIndent()

private fun changesOf(after: String): List<ApiChange> =
    ApiChangeClassifier.classify(ApiDumpParser.parse(BEFORE), ApiDumpParser.parse(after))

private fun kindsOf(after: String): List<ApiChangeKind> = changesOf(after).map { change -> change.kind }

private fun evaluate(after: String, version: SemanticVersion, announced: Boolean?): SemverResult =
    SemverEvaluator.evaluate(BEFORE, after, "1.4.0", SemanticVersion(1, 4, 0), version, announced)

class ApiChangeClassifierTest : FunSpec({
    context("the dump parser") {
        test("reads classes, supertypes and members") {
            val classes = ApiDumpParser.parse(BEFORE)

            classes.keys shouldBe setOf("com/example/Sample", "com/example/Shape", "com/example/Base")
            classes.getValue("com/example/Base").supertypes shouldBe setOf("com/example/Shape")
            classes.getValue("com/example/Shape").isInterface shouldBe true
            classes.getValue("com/example/Sample").members.size shouldBe 2
        }
    }

    context("an API") {
        test("that did not change has no changes") {
            changesOf(BEFORE).shouldBeEmpty()
        }

        test("that lost a member breaks") {
            kindsOf(BEFORE.replace("\tpublic final fun greet ()Ljava/lang/String;\n", "")) shouldBe
                listOf(ApiChangeKind.BREAKING)
        }

        test("that changed a signature breaks and adds") {
            val after = BEFORE.replace("greet ()Ljava/lang/String;", "greet (I)Ljava/lang/String;")

            kindsOf(after) shouldBe listOf(ApiChangeKind.BREAKING, ApiChangeKind.ADDITIVE)
        }

        test("that gained a class or a member only adds") {
            val greet = "\tpublic final fun greet ()Ljava/lang/String;"
            val withWave = BEFORE.replace(greet, "$greet\n\tpublic final fun wave ()V")
            val after = withWave + "public final class com/example/Extra {\n}\n"

            kindsOf(after) shouldBe listOf(ApiChangeKind.ADDITIVE, ApiChangeKind.ADDITIVE)
        }

        test("that adds an abstract member to an interface breaks its implementations") {
            val area = "public abstract fun area ()D"
            val after = BEFORE.replace(area, "$area\n\tpublic abstract fun name ()Ljava/lang/String;")

            changesOf(after).single().kind shouldBe ApiChangeKind.BREAKING
        }

        test("that makes an open class final breaks") {
            kindsOf(BEFORE.replace("public class com/example/Base", "public final class com/example/Base")) shouldBe
                listOf(ApiChangeKind.BREAKING)
        }

        test("that drops a supertype breaks") {
            val after = BEFORE.replace(" : com/example/Shape {", " {")

            kindsOf(after) shouldBe listOf(ApiChangeKind.BREAKING)
        }
    }

    context("the evaluation") {
        val removed = BEFORE.replace("\tpublic final fun greet ()Ljava/lang/String;\n", "")

        test("demands a major release for a breaking change") {
            val result = evaluate(removed, SemanticVersion(1, 5, 0), null)

            result.requiredBump shouldBe VersionBump.MAJOR
            result.isValid shouldBe false
        }

        test("accepts the major release") {
            val result = evaluate(removed, SemanticVersion(2, 0, 0, "SNAPSHOT"), true)

            result.isValid shouldBe true
        }

        test("demands a commit marked as breaking") {
            val result = evaluate(removed, SemanticVersion(2, 0, 0), false)

            result.problems.single() shouldBe
                "The API breaks, but no commit since 1.4.0 is marked as breaking with '!' or BREAKING CHANGE."
        }

        test("needs no new version for an unchanged API") {
            evaluate(BEFORE, SemanticVersion(1, 4, 0), false).isValid shouldBe true
        }
    }
})
