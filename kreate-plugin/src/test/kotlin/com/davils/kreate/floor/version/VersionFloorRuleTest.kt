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

package com.davils.kreate.floor.version

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VersionFloorRuleTest : FunSpec({
    val rule = VersionFloorRule("com.fasterxml.jackson", "2.22.3", setOf("jackson-annotations"))

    context("a module of the group") {
        test("is raised below the floor") {
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "2.17.0") shouldBe true
            rule.raises("com.fasterxml.jackson", "jackson-bom", "2.22.2") shouldBe true
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "2.22") shouldBe true
        }

        test("keeps the floor and every version above it") {
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "2.22.3") shouldBe false
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "2.23.0") shouldBe false
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "3.0.0-rc1") shouldBe false
        }

        test("keeps an excluded module") {
            rule.raises("com.fasterxml.jackson.core", "jackson-annotations", "2.10.0") shouldBe false
        }

        test("keeps a version that is no plain number, such as a range") {
            rule.raises("com.fasterxml.jackson.core", "jackson-core", "[2.0,3.0)") shouldBe false
            rule.raises("com.fasterxml.jackson.core", "jackson-core", null) shouldBe false
        }
    }

    context("a module of another group") {
        test("is never raised, even when its name shares the prefix") {
            rule.raises("com.fasterxml.jacksonfake", "jackson-core", "1.0.0") shouldBe false
            rule.raises("org.example", "jackson-core", "1.0.0") shouldBe false
        }
    }

    context("a floor") {
        test("is numeric") {
            VersionFloorRule.isNumeric("2.22.3") shouldBe true
            VersionFloorRule.isNumeric("1.85") shouldBe true
            VersionFloorRule.isNumeric("latest.release") shouldBe false
            VersionFloorRule.isNumeric("") shouldBe false
        }
    }
})
