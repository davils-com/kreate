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

package com.davils.kreate.settings.local

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LocalModuleTest : FunSpec({

    context("local module") {

        context("coordinate parsing") {

            test("accepts a group and a name") {
                LocalModule.parse(" com.example : library-core ") shouldBe
                    LocalModule("com.example", "library-core")
            }

            test("rejects anything that is not exactly one pair") {
                LocalModule.parse("com.example") shouldBe null
                LocalModule.parse("com.example:library:1.0.0") shouldBe null
                LocalModule.parse(":rise") shouldBe null
                LocalModule.parse("com.example:") shouldBe null
            }

            test("renders back to the form it was parsed from") {
                LocalModule("com.example", "library-core").coordinate shouldBe "com.example:library-core"
            }
        }
    }
})
