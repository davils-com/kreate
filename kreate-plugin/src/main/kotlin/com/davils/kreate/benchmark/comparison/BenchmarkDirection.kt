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

package com.davils.kreate.benchmark.comparison

internal enum class BenchmarkDirection {
    HIGHER_IS_BETTER,

    LOWER_IS_BETTER;

    internal companion object {
        private val HIGHER_IS_BETTER_MODES: Set<String> = setOf("thrpt", "throughput")

        fun forMode(mode: String): BenchmarkDirection {
            val measuresThroughput = mode.lowercase() in HIGHER_IS_BETTER_MODES
            if (measuresThroughput) return HIGHER_IS_BETTER
            return LOWER_IS_BETTER
        }
    }
}
