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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class JsonNumberTest : FunSpec({
    test("accepts literals of the JSON grammar") {
        listOf("0", "-0", "1.5", "1e3", "1E-3", "1.5e+3").forEach { literal ->
            JsonNumber(literal).literal shouldBe literal
        }
    }

    test("rejects anything else, so that the writer cannot emit invalid JSON") {
        listOf("", "-", "01", "1.", ".5", "1e", "NaN", "1 ", "0x10").forEach { literal ->
            shouldThrow<IllegalArgumentException> { JsonNumber(literal) }
        }
    }

    test("builds a literal from a long") {
        val number = JsonNumber.from(Long.MIN_VALUE)

        number.toLongOrNull() shouldBe Long.MIN_VALUE
    }

    test("builds a literal from a finite double") {
        val number = JsonNumber.from(1.5)

        number.toDouble() shouldBe 1.5
    }

    test("refuses non-finite doubles") {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            shouldThrow<IllegalArgumentException> { JsonNumber.from(value) }
        }
    }

    test("does not pretend a fraction or an overflow is a long") {
        val fraction = JsonNumber("1.0")
        val overflow = JsonNumber("9223372036854775808")

        fraction.toLongOrNull().shouldBeNull()
        overflow.toLongOrNull().shouldBeNull()
    }

    test("overflows to infinity as a double instead of failing") {
        JsonNumber("1e400").toDouble() shouldBe Double.POSITIVE_INFINITY
    }
})
