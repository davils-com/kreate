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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

internal fun readBenchmarkResult(entry: JsonObject): BenchmarkResult {
    val benchmark = textOf(entry["benchmark"])
    requireNotNull(benchmark) { "A benchmark report entry has no 'benchmark' name." }

    val metric = entry["primaryMetric"] as? JsonObject
    require(metric != null) { "Benchmark '$benchmark' has no 'primaryMetric'." }

    val score = finiteNumberOf(metric["score"])
    require(score != null) { "Benchmark '$benchmark' has no numeric 'score'." }

    return BenchmarkResult(
        benchmark = benchmark,
        mode = textOf(entry["mode"]).orEmpty(),
        params = readParams(entry["params"] as? JsonObject),
        score = score,
        scoreError = readDouble(metric["scoreError"]),
        scoreUnit = textOf(metric["scoreUnit"]).orEmpty()
    )
}

private fun textOf(element: JsonElement?): String? {
    val primitive = element as? JsonPrimitive ?: return null
    if (!primitive.isString) return null
    return primitive.content
}

private fun finiteNumberOf(element: JsonElement?): Double? {
    val primitive = element as? JsonPrimitive ?: return null
    if (primitive.isString) return null
    return primitive.doubleOrNull?.takeIf { number -> number.isFinite() }
}

private fun readDouble(element: JsonElement?): Double {
    val primitive = element as? JsonPrimitive ?: return Double.NaN
    return primitive.doubleOrNull ?: Double.NaN
}

private fun readParams(params: JsonObject?): Map<String, String> {
    if (params == null) return emptyMap()
    return params.mapValues { (_, value) -> renderParam(value) }
}

private fun renderParam(element: JsonElement): String = textOf(element) ?: element.toString()
