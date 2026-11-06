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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class JsonAccessorsTest : FunSpec({
    val document = JsonParser.parse(
        """{"text":"x","count":3,"ratio":1.5,"flag":true,"items":[1],"child":{"a":1},"nothing":null}"""
    ).asObject() ?: error("The fixture is an object.")

    test("reads members by their type") {
        document.string("text") shouldBe "x"
        document.long("count") shouldBe 3L
        document.double("ratio") shouldBe 1.5
        document.boolean("flag") shouldBe true
        document.array("items").orEmpty() shouldHaveSize 1
        document.obj("child")?.long("a") shouldBe 1L
    }

    test("returns null for a missing member") {
        document.string("absent").shouldBeNull()
        document.long("absent").shouldBeNull()
        document.double("absent").shouldBeNull()
        document.boolean("absent").shouldBeNull()
        document.array("absent").shouldBeNull()
        document.obj("absent").shouldBeNull()
    }

    test("returns null for a member of another type") {
        document.string("count").shouldBeNull()
        document.long("text").shouldBeNull()
        document.double("text").shouldBeNull()
        document.boolean("text").shouldBeNull()
        document.array("text").shouldBeNull()
        document.obj("text").shouldBeNull()
        document.string("nothing").shouldBeNull()
    }

    test("names every value type") {
        val typeNames = listOf(
            JsonNull,
            JsonBoolean(true),
            JsonNumber("1"),
            JsonString(""),
            JsonArray(emptyList()),
            JsonObject(emptyMap())
        ).map { value -> value.typeName }

        typeNames shouldBe listOf("null", "boolean", "number", "string", "array", "object")
    }
})
