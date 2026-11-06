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

package com.davils.kreate.json

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JsonWriterTest : FunSpec({
    val document = JsonObject(
        linkedMapOf(
            "b" to JsonArray(listOf(JsonNumber("1"), JsonBoolean(true), JsonNull)),
            "a" to JsonString("x"),
            "c" to JsonObject(emptyMap()),
            "d" to JsonArray(emptyList())
        )
    )

    test("writes compact output with sorted keys") {
        JsonWriter.compact(document) shouldBe """{"a":"x","b":[1,true,null],"c":{},"d":[]}"""
    }

    test("writes pretty output with sorted keys and a final line break") {
        JsonWriter.pretty(document) shouldBe """
            {
              "a": "x",
              "b": [
                1,
                true,
                null
              ],
              "c": {},
              "d": []
            }
        """.trimIndent() + "\n"
    }

    test("does not depend on the insertion order of the keys") {
        val reversed = JsonObject(document.members.entries.reversed().associate { it.key to it.value })

        JsonWriter.compact(reversed) shouldBe JsonWriter.compact(document)
    }

    test("keeps a number literal exactly as it was read") {
        JsonWriter.compact(JsonParser.parse("[1E+2,-0.10]")) shouldBe "[1E+2,-0.10]"
    }

    test("escapes what a JSON string may not hold raw") {
        JsonWriter.quote("\"\\\b\u000C\n\r\t\u0001") shouldBe """"\"\\\b\f\n\r\t\u0001""""
    }

    test("keeps other characters raw") {
        JsonWriter.quote("/\u00E9\u007F\uD83D\uDE00") shouldBe "\"/\u00E9\u007F\uD83D\uDE00\""
    }

    test("escapes lone surrogates so that the output stays valid UTF-8") {
        JsonWriter.quote("\uD800") shouldBe """"\ud800""""
        JsonWriter.quote("a\uDC00") shouldBe """"a\udc00""""
        JsonWriter.quote("\uD800x") shouldBe """"\ud800x""""
    }

    test("round trips hostile strings") {
        val hostile = JsonString("\u0000\"\\\uD800\u2028")

        JsonParser.parse(JsonWriter.compact(hostile)) shouldBe hostile
    }

    test("writes the same text twice") {
        JsonWriter.pretty(document) shouldBe JsonWriter.pretty(document)
    }
})
