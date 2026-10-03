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

package com.davils.kreate.benchmark.report

internal data class BenchmarkResult(
    val benchmark: String,
    val mode: String,
    val params: Map<String, String>,
    val score: Double,
    val scoreError: Double,
    val scoreUnit: String
) {
    val key: String
        get() {
            if (params.isEmpty()) return benchmark
            return benchmark + renderedParams()
        }
}

private fun BenchmarkResult.renderedParams(): String {
    val sortedParams = params.entries.sortedBy { it.key }
    return sortedParams.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
}
