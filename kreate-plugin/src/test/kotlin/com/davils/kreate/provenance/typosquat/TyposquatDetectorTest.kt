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
package com.davils.kreate.provenance.typosquat

import com.davils.kreate.assurance.finding.Finding
import com.davils.kreate.dependency.ModuleCoordinate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private val POPULAR: Set<String> = setOf(
    "com.google.guava:guava",
    "org.slf4j:slf4j-api",
    "com.fasterxml.jackson.core:jackson-databind",
    "org.jetbrains:annotations",
    "io.ktor:ktor-client-core",
    "jakarta.annotation:jakarta.annotation-api"
)

private fun findingsOf(group: String, name: String, allow: Set<String> = emptySet()): List<Finding> {
    val detector = TyposquatDetector(POPULAR, allow)
    return detector.findingsOf(listOf(ModuleCoordinate(group, name, "1.0.0")))
}

private fun rulesOf(group: String, name: String, allow: Set<String> = emptySet()): List<String> {
    val findings = findingsOf(group, name, allow)
    return findings.map { finding -> finding.ruleId }
}

class TyposquatDetectorTest : FunSpec({
    context("a group") {
        test("one character away from a popular one is reported") {
            rulesOf("com.goog1e.guava", "guava") shouldBe listOf("TYPOSQUAT_GROUP")
        }

        test("two characters away from a long popular one is reported") {
            rulesOf("com.fastrxmll.jackson.core", "jackson-databind") shouldBe listOf("TYPOSQUAT_GROUP")
        }

        test("that only differs in separators is reported") {
            rulesOf("com.faster-xml.jackson", "jackson-databind") shouldBe listOf("TYPOSQUAT_GROUP")
        }

        test("with the name of a popular one under another top-level domain is reported") {
            rulesOf("com.slf4j", "slf4j-api") shouldBe listOf("TYPOSQUAT_GROUP")
        }

        test("with the name of a popular one under another namespace that is no domain is accepted") {
            rulesOf("androidx.annotation", "annotation").shouldBeEmpty()
        }

        test("one character away from a popular one in a namespace that is no domain is reported") {
            rulesOf("jakarta.annotatlon", "jakarta.annotation-api") shouldBe listOf("TYPOSQUAT_GROUP")
        }

        test("names the group it imitates") {
            val finding = findingsOf("com.goog1e", "guava").single()

            finding.attributes["imitates"] shouldBe "com.google"
        }

        test("in the namespace of a popular owner is accepted, because Maven Central verifies the owner") {
            rulesOf("com.google.android", "guava").shouldBeEmpty()
        }

        test("with a short name is not compared at a distance of two") {
            rulesOf("io.kt", "client").shouldBeEmpty()
        }

        test("that the build allows is accepted") {
            rulesOf("com.goog1e.guava", "guava", allow = setOf("com.goog1e.guava")).shouldBeEmpty()
        }
    }

    context("an artifact") {
        test("of a popular module under a foreign group is reported") {
            rulesOf("io.github.someone", "guava") shouldBe listOf("TYPOSQUAT_ARTIFACT")
        }

        test("one character away from a popular one under a foreign group is reported") {
            rulesOf("io.github.someone", "jackson-databin") shouldBe listOf("TYPOSQUAT_ARTIFACT")
        }

        test("with a generic name is not reported") {
            rulesOf("io.github.someone", "annotations").shouldBeEmpty()
        }

        test("that the build allows is accepted") {
            rulesOf("io.github.someone", "guava", allow = setOf("io.github.someone:guava")).shouldBeEmpty()
        }
    }

    context("a popular module") {
        test("is accepted") {
            rulesOf("com.google.guava", "guava").shouldBeEmpty()
        }

        test("of the built-in list is known") {
            ("com.google.guava:guava" in PopularModules.coordinates) shouldBe true
        }
    }
})
