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
package com.davils.kreate.advisory.version

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe

class MavenVersionTest : FunSpec({
    context("the order of versions") {
        test("follows the release line") {
            val versions = listOf(
                "1.0.0-alpha1",
                "1.0.0-beta2",
                "1.0.0-M3",
                "1.0.0-RC1",
                "1.0.0-SNAPSHOT",
                "1.0.0",
                "1.0.1",
                "1.2.0",
                "1.10.0",
                "2.0.0"
            )

            val parsed = versions.map(::MavenVersion)

            parsed shouldBeSortedWith { first, second -> first.compareTo(second) }
        }

        test("compares numbers as numbers") {
            (MavenVersion("2.10") > MavenVersion("2.9")) shouldBe true
        }

        test("treats a missing trailing zero and a final marker as the same release") {
            MavenVersion("1.0").compareTo(MavenVersion("1.0.0")) shouldBe 0
            MavenVersion("1.0.0").compareTo(MavenVersion("1.0.0.Final")) shouldBe 0
        }

        test("orders a build qualifier after the release it extends") {
            (MavenVersion("1.0.0-jre") > MavenVersion("1.0.0")) shouldBe true
        }

        test("orders a pre-release of a fix after the older release") {
            (MavenVersion("2.4.20-Beta1") > MavenVersion("2.4.0")) shouldBe true
            (MavenVersion("2.4.20-Beta1") < MavenVersion("2.4.20")) shouldBe true
        }

        test("understands a very long number") {
            (MavenVersion("1.20261106123456789") > MavenVersion("1.2")) shouldBe true
        }
    }
})
