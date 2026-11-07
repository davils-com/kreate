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
package com.davils.kreate.governance.add

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ModuleRequestTest : FunSpec({
    context("a module request") {
        test("may name a version") {
            val expected = ModuleRequest("com.squareup.okio", "okio", "3.9.0")

            ModuleRequest.parse("com.squareup.okio:okio:3.9.0") shouldBe expected
            ModuleRequest.parse("com.squareup.okio:okio") shouldBe ModuleRequest("com.squareup.okio", "okio", null)
        }

        listOf("okio", "a:b:c:d", "a:b:1.+", "a:b:latest.release", "a b:c", "a:\"b\"").forEach { notation ->
            test("refuses '$notation'") {
                shouldThrow<IllegalArgumentException> { ModuleRequest.parse(notation) }
            }
        }
    }
})
