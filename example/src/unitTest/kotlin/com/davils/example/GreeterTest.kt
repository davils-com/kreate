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

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {

    @Test
    fun greetsByName() {
        assertEquals("Hello, Kreate!", Greeter().greet("Kreate"))
    }

    @Test
    fun fallsBackWhenNameIsBlank() {
        // Covers the other side of the condition. Without this the class still reaches full
        // line coverage, which is the gap a branch coverage bound is there to catch.
        assertEquals("Hello!", Greeter().greet("  "))
    }
}
