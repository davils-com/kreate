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

package com.davils.kreate.coverage

/**
 * The unit of code a coverage measurement counts.
 *
 * The three units answer different questions about the same test run, and a build that only
 * tracks one of them can be misled. Line coverage says which lines ran; branch coverage says
 * whether both sides of every condition ran. A suite can reach full line coverage while never
 * taking the `false` path of a single `if`.
 *
 * @since 2.2.0
 */
public enum class CoverageUnit {
    /**
     * Counts lines of code. The unit most people mean when they say "coverage".
     * @since 2.2.0
     */
    LINE,

    /**
     * Counts JVM bytecode instructions. Finer-grained than [LINE] and insensitive to how the
     * source is formatted, at the cost of being harder to relate back to the code.
     * @since 2.2.0
     */
    INSTRUCTION,

    /**
     * Counts the branches of conditional statements. The unit that catches a test suite which
     * exercises every line but only ever one outcome per condition.
     * @since 2.2.0
     */
    BRANCH
}
