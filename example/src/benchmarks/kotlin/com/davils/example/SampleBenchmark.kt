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

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

/**
 * A benchmark that measures nothing worth knowing.
 *
 * It exists so that this project exercises the same pipeline a consumer would: Kreate
 * creates the `benchmarks` source set, associates it with `main`, applies `allopen` so JMH
 * can subclass this `@State` class, and compares the run against `benchmarks/baseline.json`.
 *
 * @since 2.1.0
 */
@State(Scope.Benchmark)
class SampleBenchmark {
    private val values = (1..VALUE_COUNT).toList()

    /**
     * Sums a small list, which is enough work to produce a stable number quickly.
     *
     * @return The sum.
     * @since 2.1.0
     */
    @Benchmark
    fun sum(): Int = values.sum()

    private companion object {
        const val VALUE_COUNT = 100
    }
}
