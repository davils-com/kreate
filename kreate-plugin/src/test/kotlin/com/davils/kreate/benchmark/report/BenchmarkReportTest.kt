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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class BenchmarkReportTest : FunSpec({
    val kotlinxReport = """
        [
          {
            "benchmark" : "com.example.Bench.parse",
            "mode" : "thrpt",
            "configurationName" : "main",
            "warmupIterations" : 5,
            "params" : {
                  
            },
            "advanced" : {
                  "jvmForks" : "1"
            },
            "primaryMetric" : {
               "score": 1234.5,
               "scoreError": 12.5,
               "scoreConfidence" : [
                  1222.0,
                  1247.0
               ],
               "scorePercentiles" : {
                  "100.00" : 1240.0
               },
               "scoreUnit" : "ops/s",
               "rawData" : [
                   [
                     1230.0,
                     1239.0
                   ]
               ]
            },
            "secondaryMetrics" : {
            }
          }
        ]
    """.trimIndent()

    context("Benchmark report parsing") {
        test("reads the fields the gate depends on") {
            val result = BenchmarkReport.parse(kotlinxReport).single()

            result.benchmark shouldBe "com.example.Bench.parse"
            result.mode shouldBe "thrpt"
            result.score shouldBe 1234.5
            result.scoreError shouldBe 12.5
            result.scoreUnit shouldBe "ops/s"
            result.params shouldBe emptyMap()
        }

        test("reads a NaN score error, which is not valid JSON") {
            val singleIterationReportWithBareNaN = kotlinxReport.replace("\"scoreError\": 12.5", "\"scoreError\": NaN")

            val result = BenchmarkReport.parse(singleIterationReportWithBareNaN).single()

            result.scoreError.isNaN() shouldBe true
            result.score shouldBe 1234.5
        }

        test("keeps parameters as part of the benchmark identity") {
            val parameterised = """
                [
                  {
                    "benchmark" : "com.example.Bench.parse",
                    "mode" : "thrpt",
                    "params" : {
                          "size" : "1024",
                          "mode" : "fast"
                    },
                    "primaryMetric" : {
                       "score": 1.0,
                       "scoreError": 0.1,
                       "scoreUnit" : "ops/s"
                    }
                  }
                ]
            """.trimIndent()

            val result = BenchmarkReport.parse(parameterised).single()

            result.params shouldBe mapOf("size" to "1024", "mode" to "fast")
            val keySortedByParameterName = result.key
            keySortedByParameterName shouldBe "com.example.Bench.parse [mode=fast, size=1024]"
        }

        test("uses the plain name as the key when there are no parameters") {
            BenchmarkReport.parse(kotlinxReport).single().key shouldBe "com.example.Bench.parse"
        }

        test("reads the report JMH writes for a JVM target") {
            val jmhReportWithQuotedNaNAndNoParams = """
                [
                    {
                        "jmhVersion" : "1.37",
                        "benchmark" : "com.example.SampleBenchmark.sum",
                        "mode" : "thrpt",
                        "threads" : 1,
                        "forks" : 1,
                        "jvm" : "/usr/lib/jvm/java-17-openjdk/bin/java",
                        "jvmArgs" : [ "-Dfile.encoding=UTF-8" ],
                        "measurementIterations" : 2,
                        "primaryMetric" : {
                            "score" : 1.8033110268836617E7,
                            "scoreError" : "NaN",
                            "scoreConfidence" : [ "NaN", "NaN" ],
                            "scoreUnit" : "ops/s"
                        },
                        "secondaryMetrics" : {
                        }
                    }
                ]
            """.trimIndent()

            val result = BenchmarkReport.parse(jmhReportWithQuotedNaNAndNoParams).single()

            result.benchmark shouldBe "com.example.SampleBenchmark.sum"
            result.score shouldBe 1.8033110268836617E7
            result.scoreError.isNaN() shouldBe true
            result.scoreUnit shouldBe "ops/s"
            result.params shouldBe emptyMap()
        }

        test("renders a canonical baseline that survives a round trip") {
            val results = BenchmarkReport.parse(kotlinxReport)

            val baselineWithoutMachineDetails = BenchmarkReport.render(results)

            baselineWithoutMachineDetails shouldNotContain "rawData"
            baselineWithoutMachineDetails shouldNotContain "jvmArgs"
            BenchmarkReport.parse(baselineWithoutMachineDetails) shouldBe results
        }

        test("renders a non-finite score error as valid JSON") {
            val result = BenchmarkResult(
                benchmark = "com.example.Bench.run",
                mode = "thrpt",
                params = mapOf("size" to "8"),
                score = 1.0,
                scoreError = Double.NaN,
                scoreUnit = "ops/s"
            )

            val rendered = BenchmarkReport.render(listOf(result))

            rendered shouldContain """"scoreError" : "NaN""""
            BenchmarkReport.parse(rendered).single() shouldBe result
        }

        test("reads an empty report") {
            BenchmarkReport.parse("[]") shouldBe emptyList()
        }

        test("rejects a document that is not a report") {
            shouldThrow<IllegalArgumentException> {
                BenchmarkReport.parse("""{ "benchmark": "x" }""")
            }.message.orEmpty() shouldContain "must be a JSON array"

            shouldThrow<IllegalArgumentException> {
                BenchmarkReport.parse("not json at all {")
            }
        }

        test("names the benchmark whose measurement is unusable") {
            val broken = kotlinxReport.replace("\"score\": 1234.5,", "")

            val message = shouldThrow<IllegalArgumentException> { BenchmarkReport.parse(broken) }.message
            message.orEmpty() shouldContain "com.example.Bench.parse"
        }
    }
})
