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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InflatedVersionsTest : FunSpec({
    context("a version") {
        listOf("99.0.0", "51", "1000.1").forEach { version ->
            test("$version is inflated") {
                InflatedVersions.isInflated(version) shouldBe true
            }
        }

        listOf("2.4.21", "50.0.0", "2024.3.1", "202403", "20250517", "1.0-SNAPSHOT", "r09", "").forEach { version ->
            test("'$version' is not inflated") {
                InflatedVersions.isInflated(version) shouldBe false
            }
        }
    }
})
