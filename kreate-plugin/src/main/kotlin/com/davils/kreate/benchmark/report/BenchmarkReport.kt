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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object BenchmarkReport {
    private const val INVALID_JSON_MESSAGE = "The benchmark report is not valid JSON."

    fun parse(json: String): List<BenchmarkResult> {
        val parsed = parseJson(json)

        require(parsed is JsonArray) {
            "A benchmark report must be a JSON array of measurements, but the document is " +
                "${describe(parsed)}."
        }

        return parsed.map { entry ->
            require(entry is JsonObject) { "Every entry of a benchmark report must be an object." }
            readBenchmarkResult(entry)
        }
    }

    fun render(results: List<BenchmarkResult>): String =
        results.sortedBy { it.key }.joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n]\n",
            transform = ::renderResult
        )

    private fun parseJson(json: String): JsonElement {
        require(json.isNotEmpty()) { INVALID_JSON_MESSAGE }

        return try {
            Json.parseToJsonElement(json)
        } catch (cause: SerializationException) {
            throw IllegalArgumentException(INVALID_JSON_MESSAGE, cause)
        }
    }

    private fun describe(element: JsonElement): String = when (element) {
        is JsonArray -> "an array"
        is JsonObject -> "an object"
        is JsonPrimitive -> "a value"
    }

    private fun quoted(text: String): String = JsonPrimitive(text).toString()

    private fun renderResult(result: BenchmarkResult): String {
        val sortedParams = result.params.entries.sortedBy { it.key }
        val params = sortedParams.joinToString(", ") { "${quoted(it.key)} : ${quoted(it.value)}" }

        return listOf(
            "  {",
            """    "benchmark" : ${quoted(result.benchmark)},""",
            """    "mode" : ${quoted(result.mode)},""",
            """    "params" : { $params },""",
            """    "primaryMetric" : {""",
            """      "score" : ${result.score},""",
            """      "scoreError" : ${renderDouble(result.scoreError)},""",
            """      "scoreUnit" : ${quoted(result.scoreUnit)}""",
            "    }",
            "  }"
        ).joinToString("\n")
    }

    private fun renderDouble(value: Double): String {
        if (!value.isFinite()) return "\"$value\""
        return value.toString()
    }
}
