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

package com.davils.example

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GreeterTest : FunSpec({
    context("greet") {
        test("greets an ordinary name") {
            val greeting = Greeter().greet("Kreate")

            greeting shouldBe "Hello, Kreate!"
        }

        test("falls back to a generic greeting for a blank name") {
            val greeting = Greeter().greet("  ")

            greeting shouldBe "Hello!"
        }
    }
})
