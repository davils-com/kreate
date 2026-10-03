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

/**
 * Builds greetings. A piece of pure Kotlin with one branch, so that the coverage example has
 * something to measure that does not depend on the native library.
 *
 * @since 2.2.0
 */
public class Greeter {
    /**
     * Builds a greeting for the given name.
     *
     * @param name The name to greet. A blank name falls back to a generic greeting.
     * @return The greeting.
     * @since 2.2.0
     */
    public fun greet(name: String): String {
        if (name.isBlank()) return ANONYMOUS_GREETING
        return "Hello, $name!"
    }

    private companion object {
        private const val ANONYMOUS_GREETING: String = "Hello!"
    }
}
