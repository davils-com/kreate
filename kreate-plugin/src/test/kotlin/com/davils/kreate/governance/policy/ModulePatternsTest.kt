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
package com.davils.kreate.governance.policy

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ModulePatternsTest : FunSpec({
    context("a module pattern") {
        test("names one module") {
            matchesModule("log4j:log4j", "log4j", "log4j") shouldBe true
            matchesModule("log4j:log4j", "log4j", "other") shouldBe false
        }

        test("names every artifact of a group with a star") {
            matchesModule("org.example:*", "org.example", "anything") shouldBe true
            matchesModule("org.example:*", "org.example.sub", "anything") shouldBe false
        }

        test("names a group with its subgroups, but not a look-alike") {
            matchesModule("org.example", "org.example.sub", "lib") shouldBe true
            matchesModule("org.example", "org.examples", "lib") shouldBe false
        }
    }

    context("a version") {
        listOf("1.+", "+", "latest.release", "[1.0,2.0)", "(,1.0]", "]1.0,2.0[").forEach { version ->
            test("$version is dynamic") {
                VersionKinds.isDynamic(version) shouldBe true
            }
        }

        listOf("1.0", "2.4.21", "1.0-SNAPSHOT").forEach { version ->
            test("$version is not dynamic") {
                VersionKinds.isDynamic(version) shouldBe false
            }
        }

        test("ending in -SNAPSHOT changes") {
            VersionKinds.isChanging("1.0-SNAPSHOT") shouldBe true
            VersionKinds.isChanging("1.0") shouldBe false
        }
    }
})
