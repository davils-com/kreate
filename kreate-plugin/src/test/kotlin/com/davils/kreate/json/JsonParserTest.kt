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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private val ACCEPTED_DOCUMENTS = listOf(
    "[[]   ]", """[""]""", "[]", """["a"]""", "[false]", """[null, 1, "1", {}]""", "[null]", "[1\n]", " [1]",
    "[1,null,null,null,2]", "[2] ", "[123e65]", "[0e+1]", "[0e1]", "[ 4]", "[-0.000000000000000000000001]",
    "[20e1]", "[-0]", "[-123]", "[-1]", "[1E22]", "[1E-2]", "[1E+2]", "[123e45]", "[123.456e78]", "[1e-2]",
    "[1e+2]", "[123]", "[123.456789]", """{"asd":"sdf", "dfg":"fgh"}""", """{"asd":"sdf"}""",
    """{"a":"b","a":"c"}""", """{"a":"b","a":"b"}""", "{}", """{"":0}""", """{"foo\u0000bar": 42}""",
    """{ "min": -1.0e+28, "max": 1.0e+28 }""", """{"x":[{"id": "xxx"}], "id": "xxx"}""", """{"a":[]}""",
    """{"title":"\u041f\u043e\u043b" }""", "{\n" + """"a": "b"""" + "\n}", """["\u0060\u012a\u12AB"]""",
    """["\uD801\udc37"]""", """["\ud83d\ude39\ud83d\udc8d"]""", """["\"\\\/\b\f\n\r\t"]""", """["\\u0000"]""",
    """["\""]""", """["a/*b*/c/*d//e"]""", """["\\a"]""", """["\\n"]""", """["\u0012"]""", """["\uFFFF"]""",
    """[ "asd"]""", """["\uDBFF\uDFFF"]""", "[\"new\u00A0line\"]", "[\"\uDBFF\uDFFF\"]", "[\"\uFFFF\"]",
    """["\u0000"]""", """["\u002c"]""", "[\"\u03C0\"]", "[\"asd \"]", "\" \"", """["\uD834\uDd1e"]""",
    "[\"\u2028\"]", "[\"\u2029\"]", """["new\u000Aline"]""", "[\"\u007F\"]", "false", "42", "-0.1", "null",
    "\"asd\"", "true", "\"\"", "[\"a\"]\n", "[true]", " [] ", "\t\r\n[]\t\r\n"
)

private val REJECTED_DOCUMENTS = listOf(
    "[1 true]", "[a\u00E5]", """["": 1]""", """[""],""", "[,1]", "[1,,2]", """["x",,]""", """["x"]]""", """["",]""",
    """["x"""", "[x", "[3[4]]", "[1:2]", "[,]", "[-]", """[   , ""]""", "[\"a\",\n4\n,1,", "[1,]", "[1,,]",
    "[\"a\"\u000C]", "[*]", """[""""", "[1,", "[1,\n1\n,1", "[{}",
    "[++1234]", "[+1]", "[+Inf]", "[-01]", "[-1.0.]", "[-2.]", "[-NaN]", "[.-1]", "[.2e-3]", "[0.1.2]", "[0.3e+]",
    "[0.3e]", "[0.e1]", "[0E+]", "[0E]", "[0e+]", "[0e]", "[1.0e+]", "[1.0e-]", "[1.0e]", "[1 000.0]", "[1eE2]",
    "[2.e+3]", "[2.e-3]", "[2.e3]", "[9.e+]", "[Inf]", "[NaN]", "[\uFF11]", "[1+2]", "[0x1]", "[0x42]", "[Infinity]",
    "[0e+-1]", "[-Infinity]", "[-foo]", "[- 1]", "[-012]", "[-.123]", "[-1x]", "[1ea]", "[1e\u00E5]", "[1.]",
    "[.123]", "[1.2a-3]", "[1.8011670033376514H-308]", "[012]",
    """{"x", null}""", """{"x"::"b"}""", "{\uD83C\uDDE8}", """{"a":"a" 123}""", "{key: 'value'}", """{"a" b}""",
    """{:"b"}""", """{"a" "b"}""", """{"a":""", """{"a"""", "{1:1}", "{9999E9999:1}", "{null:null,null:null}",
    """{"id":0,,,,,}""", "{'a':0}", """{"id":0,}""", """{"a":"b"}/**/""", """{"a":"b"}/**//""", """{"a":"b"}//""",
    """{"a":"b"}/""", """{"a":"b",,"c":"d"}""", """{a: "b"}""", """{"a":"a""", """{ "foo" : "bar", "a" }""",
    """{"a":"b"}#""",
    """["\uD800\"]""", """["\uD800\u"]""", """["\uD800\u1"]""", """["\uD800\u1x"]""", "[\u00E9]", """["\"]""",
    """["\x00"]""", """["\\\"]""", "[\"\\\t\"]", "[\"\\\uD83C\uDF00\"]", """["\u00A"]""", """["\uD834\uDd"]""",
    """["\uD800\uD800\x"]""", """["\uqqqq"]""", """["\u+123"]""", """["\a"]""", """[\u0020"asd"]""", "[\\n]",
    "\"", "['single quote']", "abc", """["\""", "[\"new\nline\"]", "[\"\t\"]", "\"\\UA66D\"", "\"\"x", "\"\\",
    "\"\\u12", "\"\u0001\"",
    "", " ", "<.>", "[<null>]", "[1]x", "[1]]", """["asd]""", "a\u00E5", "true x", "1]", """{"x": true,""", "[True]",
    "nul", "tru", "fals", "]", "}", ",", ":", "[{", "{[", "\uFEFF", "[\u00A0]", "[\u000C]", "\u000C[]", "[]\u000C"
)

class JsonParserTest : FunSpec({
    context("documents the grammar accepts") {
        ACCEPTED_DOCUMENTS.forEachIndexed { index, document ->
            test("accepts document #$index") {
                JsonParser.parse(document).shouldBeInstanceOf<JsonValue>()
            }
        }
    }

    context("documents the grammar rejects") {
        REJECTED_DOCUMENTS.forEachIndexed { index, document ->
            test("rejects document #$index") {
                shouldThrow<JsonParseException> { JsonParser.parse(document) }
            }
        }
    }

    context("decoding") {
        test("decodes every value type") {
            val document = """{"a":[1,2.5,"x",true,false,null],"b":{}}"""

            val parsed = JsonParser.parse(document)

            parsed shouldBe JsonObject(
                mapOf(
                    "a" to JsonArray(
                        listOf(
                            JsonNumber("1"),
                            JsonNumber("2.5"),
                            JsonString("x"),
                            JsonBoolean(true),
                            JsonBoolean(false),
                            JsonNull
                        )
                    ),
                    "b" to JsonObject(emptyMap())
                )
            )
        }

        test("decodes the simple escapes") {
            JsonParser.parse(""""\"\\\/\b\f\n\r\t"""") shouldBe JsonString("\"\\/\b\u000C\n\r\t")
        }

        test("decodes unicode escapes of either case") {
            JsonParser.parse(""""\u00e9\u00E9"""") shouldBe JsonString("\u00E9\u00E9")
        }

        test("joins an escaped surrogate pair") {
            JsonParser.parse(""""\uD83D\uDE00"""") shouldBe JsonString("\uD83D\uDE00")
        }

        test("keeps a lone surrogate, which is implementation defined") {
            JsonParser.parse(""""\uD800"""") shouldBe JsonString("\uD800")
        }

        test("lets the last duplicate key win") {
            JsonParser.parse("""{"a":1,"a":2}""") shouldBe JsonObject(mapOf("a" to JsonNumber("2")))
        }

        test("keeps a number literal the double type cannot hold") {
            val parsed = JsonParser.parse("[123123e100000]")

            parsed shouldBe JsonArray(listOf(JsonNumber("123123e100000")))
        }

        test("ignores one leading byte order mark") {
            JsonParser.parse("\uFEFF[]") shouldBe JsonArray(emptyList())
        }
    }

    context("limits") {
        test("rejects nesting that would exhaust the stack") {
            val hostile = "[".repeat(100_000)

            val failure = shouldThrow<JsonParseException> { JsonParser.parse(hostile) }

            failure.message shouldContain "deeper than"
            failure.position shouldBe JsonParserOptions.DEFAULT_MAX_DEPTH
        }

        test("rejects hostile object nesting") {
            val hostile = """{"a":""".repeat(100_000)

            shouldThrow<JsonParseException> { JsonParser.parse(hostile) }
        }

        test("accepts nesting up to the limit") {
            val options = JsonParserOptions(maxDepth = 3)

            JsonParser.parse("[[[]]]", options) shouldBe JsonArray(listOf(JsonArray(listOf(JsonArray(emptyList())))))
            shouldThrow<JsonParseException> { JsonParser.parse("[[[[]]]]", options) }
        }

        test("rejects oversized text before parsing it") {
            val options = JsonParserOptions(maxSize = 4)

            JsonParser.parse("[ ] ", options) shouldBe JsonArray(emptyList())
            val failure = shouldThrow<JsonParseException> { JsonParser.parse("[   ]", options) }
            failure.position shouldBe 0
        }

        test("rejects oversized bytes before decoding them") {
            val options = JsonParserOptions(maxSize = 2)
            val hostile = ByteArray(3) { 0xFF.toByte() }

            val failure = shouldThrow<JsonParseException> { JsonParser.parse(hostile, options) }

            failure.message shouldContain "larger than"
        }
    }

    context("bytes") {
        test("decodes UTF-8") {
            val bytes = "\"\u00E9\"".toByteArray()

            JsonParser.parse(bytes) shouldBe JsonString("\u00E9")
        }

        test("rejects invalid UTF-8") {
            val truncatedSequence = byteArrayOf(0x22, 0xC3.toByte(), 0x22)

            val failure = shouldThrow<JsonParseException> { JsonParser.parse(truncatedSequence) }

            failure.message shouldContain "UTF-8"
        }
    }

    context("non-finite numbers") {
        val options = JsonParserOptions(nonFiniteAsNull = true)

        listOf("NaN", "Infinity", "-Infinity").forEach { literal ->
            test("reads $literal as null when asked to") {
                JsonParser.parse("""{"x":[$literal]}""", options) shouldBe
                    JsonObject(mapOf("x" to JsonArray(listOf(JsonNull))))
            }
        }

        test("still parses ordinary negative numbers") {
            JsonParser.parse("-1", options) shouldBe JsonNumber("-1")
        }

        test("leaves a literal inside a string alone") {
            JsonParser.parse("""{"a: NaN":": NaN"}""", options) shouldBe
                JsonObject(mapOf("a: NaN" to JsonString(": NaN")))
        }
    }
})
