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

import groovy.json.JsonException
import groovy.json.JsonSlurper

internal object BenchmarkReport {
    private const val INVALID_JSON_MESSAGE = "The benchmark report is not valid JSON."
    private val NON_FINITE_LITERAL = Regex(""":\s*(NaN|-?Infinity)\b""")

    fun parse(json: String): List<BenchmarkResult> {
        val sanitized = NON_FINITE_LITERAL.replace(json) { ": null" }
        val parsed = parseJson(sanitized)

        require(parsed is List<*>) {
            "A benchmark report must be a JSON array of measurements, but the document is " +
                "a ${parsed?.javaClass?.simpleName ?: "null"}."
        }

        return parsed.map { entry ->
            require(entry is Map<*, *>) { "Every entry of a benchmark report must be an object." }
            readResult(entry)
        }
    }

    fun render(results: List<BenchmarkResult>): String =
        results.sortedBy { it.key }.joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n]\n",
            transform = ::renderResult
        )

    private fun parseJson(json: String): Any? {
        require(json.isNotEmpty()) { INVALID_JSON_MESSAGE }

        return try {
            JsonSlurper().parseText(json)
        } catch (cause: JsonException) {
            throw IllegalArgumentException(INVALID_JSON_MESSAGE, cause)
        }
    }

    private fun readResult(entry: Map<*, *>): BenchmarkResult {
        val benchmark = entry["benchmark"] as? String
        requireNotNull(benchmark) { "A benchmark report entry has no 'benchmark' name." }

        val metric = entry["primaryMetric"] as? Map<*, *>
        require(metric != null) { "Benchmark '$benchmark' has no 'primaryMetric'." }

        val score = (metric["score"] as? Number)?.toDouble()
        require(score != null) { "Benchmark '$benchmark' has no numeric 'score'." }

        return BenchmarkResult(
            benchmark = benchmark,
            mode = entry["mode"] as? String ?: "",
            params = readParams(entry["params"]),
            score = score,
            scoreError = readDouble(metric["scoreError"]),
            scoreUnit = metric["scoreUnit"] as? String ?: ""
        )
    }

    private fun readDouble(value: Any?): Double {
        when (value) {
            is Number -> return value.toDouble()
            is String -> return value.toDoubleOrNull() ?: Double.NaN
        }
        return Double.NaN
    }

    private fun readParams(params: Any?): Map<String, String> {
        if (params !is Map<*, *>) return emptyMap()

        return params.entries
            .filter { it.key is String }
            .associate { (key, value) -> key as String to value.toString() }
    }

    private fun renderResult(result: BenchmarkResult): String {
        val sortedParams = result.params.entries.sortedBy { it.key }
        val params = sortedParams.joinToString(", ") { """"${it.key}" : "${it.value}"""" }

        return listOf(
            "  {",
            """    "benchmark" : "${result.benchmark}",""",
            """    "mode" : "${result.mode}",""",
            """    "params" : { $params },""",
            """    "primaryMetric" : {""",
            """      "score" : ${result.score},""",
            """      "scoreError" : ${renderDouble(result.scoreError)},""",
            """      "scoreUnit" : "${result.scoreUnit}"""",
            "    }",
            "  }"
        ).joinToString("\n")
    }

    private fun renderDouble(value: Double): String {
        if (!value.isFinite()) return "\"$value\""
        return value.toString()
    }
}
