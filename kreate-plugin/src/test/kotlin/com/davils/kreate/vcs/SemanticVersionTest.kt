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
package com.davils.kreate.vcs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private fun version(text: String): SemanticVersion = SemanticVersion.parse(text) ?: error("no version: $text")

class SemanticVersionTest : FunSpec({
    context("a semantic version") {
        test("is read with its pre-release and without its build") {
            SemanticVersion.parse("1.2.3-rc.1+build.5") shouldBe SemanticVersion(1, 2, 3, "rc.1")
        }

        listOf("1.2", "v1.2.3", "1.2.3.4", "01.2", "").forEach { text ->
            test("'$text' is none") {
                SemanticVersion.parse(text).shouldBeNull()
            }
        }

        test("orders as semantic versioning prescribes") {
            val ordered = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2",
                "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.10.0", "2.0.0").map(::version)

            ordered.shuffled().sorted() shouldBe ordered
        }
    }

    context("a bump") {
        test("raises major, minor or patch") {
            VersionBump.MAJOR.apply(version("1.4.2")) shouldBe version("2.0.0")
            VersionBump.MINOR.apply(version("1.4.2")) shouldBe version("1.5.0")
            VersionBump.PATCH.apply(version("1.4.2")) shouldBe version("1.4.3")
        }

        test("moves one place to the right before 1.0.0") {
            VersionBump.MAJOR.apply(version("0.4.2")) shouldBe version("0.5.0")
            VersionBump.MINOR.apply(version("0.4.2")) shouldBe version("0.4.3")
        }

        test("releases a pre-release") {
            VersionBump.PATCH.apply(version("2.0.0-beta.1")) shouldBe version("2.0.0")
        }
    }

    context("the project version") {
        test("is a release only without a pre-release") {
            releaseVersionOf("4.0.0") shouldBe version("4.0.0")
            releaseVersionOf("4.1.0-SNAPSHOT").shouldBeNull()
            coreVersionOf("4.1.0-SNAPSHOT") shouldBe version("4.1.0")
        }
    }
})
