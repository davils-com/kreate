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

import com.davils.kreate.json.JsonArray
import com.davils.kreate.json.JsonNumber
import com.davils.kreate.json.JsonObject
import com.davils.kreate.json.JsonParseException
import com.davils.kreate.json.JsonParser
import com.davils.kreate.json.JsonParserOptions
import com.davils.kreate.json.JsonString
import com.davils.kreate.json.JsonValue
import com.davils.kreate.json.JsonWriter
import com.davils.kreate.json.asString
import com.davils.kreate.json.double
import com.davils.kreate.json.obj
import com.davils.kreate.json.string
import com.davils.kreate.json.typeName

internal object BenchmarkReport {
    private const val INVALID_JSON_MESSAGE = "The benchmark report is not valid JSON."
    private val PARSER_OPTIONS = JsonParserOptions(maxSize = Int.MAX_VALUE, nonFiniteAsNull = true)

    fun parse(json: String): List<BenchmarkResult> {
        val parsed = parseJson(json)

        require(parsed is JsonArray) {
            "A benchmark report must be a JSON array of measurements, but the document is " +
                "a ${parsed.typeName}."
        }

        return parsed.elements.map { entry ->
            require(entry is JsonObject) { "Every entry of a benchmark report must be an object." }
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

    private fun parseJson(json: String): JsonValue {
        require(json.isNotEmpty()) { INVALID_JSON_MESSAGE }

        return try {
            JsonParser.parse(json, PARSER_OPTIONS)
        } catch (cause: JsonParseException) {
            throw IllegalArgumentException(INVALID_JSON_MESSAGE, cause)
        }
    }

    private fun readResult(entry: JsonObject): BenchmarkResult {
        val benchmark = entry.string("benchmark")
        requireNotNull(benchmark) { "A benchmark report entry has no 'benchmark' name." }

        val metric = entry.obj("primaryMetric")
        require(metric != null) { "Benchmark '$benchmark' has no 'primaryMetric'." }

        val score = metric.double("score")
        require(score != null) { "Benchmark '$benchmark' has no numeric 'score'." }

        return BenchmarkResult(
            benchmark = benchmark,
            mode = entry.string("mode") ?: "",
            params = readParams(entry.obj("params")),
            score = score,
            scoreError = readDouble(metric.members["scoreError"]),
            scoreUnit = metric.string("scoreUnit") ?: ""
        )
    }

    private fun readDouble(value: JsonValue?): Double {
        if (value is JsonNumber) return value.toDouble()
        val text = value.asString() ?: return Double.NaN
        return text.toDoubleOrNull() ?: Double.NaN
    }

    private fun readParams(params: JsonObject?): Map<String, String> {
        if (params == null) return emptyMap()
        return params.members.mapValues { (_, value) -> renderParam(value) }
    }

    private fun renderParam(value: JsonValue): String {
        if (value is JsonString) return value.value
        return JsonWriter.compact(value)
    }

    private fun renderResult(result: BenchmarkResult): String {
        val sortedParams = result.params.entries.sortedBy { it.key }
        val params = sortedParams.joinToString(", ") { "${JsonWriter.quote(it.key)} : ${JsonWriter.quote(it.value)}" }

        return listOf(
            "  {",
            """    "benchmark" : ${JsonWriter.quote(result.benchmark)},""",
            """    "mode" : ${JsonWriter.quote(result.mode)},""",
            """    "params" : { $params },""",
            """    "primaryMetric" : {""",
            """      "score" : ${result.score},""",
            """      "scoreError" : ${renderDouble(result.scoreError)},""",
            """      "scoreUnit" : ${JsonWriter.quote(result.scoreUnit)}""",
            "    }",
            "  }"
        ).joinToString("\n")
    }

    private fun renderDouble(value: Double): String {
        if (!value.isFinite()) return "\"$value\""
        return value.toString()
    }
}
