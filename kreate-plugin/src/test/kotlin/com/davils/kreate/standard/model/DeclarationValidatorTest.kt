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
package com.davils.kreate.standard.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain

private const val YEAR: Long = 365L * 24 * 60 * 60

class DeclarationValidatorTest : FunSpec({
    test("accepts declarations a body can check without an interval") {
        val declarations = listOf(StandardDeclaration(StandardIds.rfc(9562)), StandardDeclaration(StandardIds.w3c("x")))

        DeclarationValidator.problemsOf(declarations, null).shouldBeEmpty()
    }

    test("refuses a standard declared twice") {
        val twice = listOf(StandardDeclaration(StandardIds.rfc(9562)), StandardDeclaration(StandardIds.rfc(9562)))

        DeclarationValidator.problemsOf(twice, null) shouldContainExactly listOf("rfc:9562 is declared more than once.")
    }

    test("needs an interval for what no body can check") {
        val din = StandardDeclaration(StandardIds.din("DIN 5008"))

        DeclarationValidator.problemsOf(listOf(din), null).single() shouldContain "din:5008 needs reviewEvery"
        DeclarationValidator.problemsOf(listOf(din), YEAR).shouldBeEmpty()
        DeclarationValidator.problemsOf(listOf(din.copy(reviewEverySeconds = YEAR)), null).shouldBeEmpty()
    }

    test("refuses an interval that is not positive") {
        val rfc = StandardDeclaration(StandardIds.rfc(1), reviewEverySeconds = 0)

        DeclarationValidator.problemsOf(listOf(rfc), -1) shouldHaveSize 2
    }

    test("refuses tags and urls where they mean nothing") {
        val rfc = StandardDeclaration(StandardIds.rfc(1), tags = "v.*", url = "https://example.org")

        DeclarationValidator.problemsOf(listOf(rfc), null) shouldHaveSize 2
    }

    test("refuses tags that do not compile") {
        val github = StandardDeclaration(StandardIds.github("a/b"), tags = "v(")

        DeclarationValidator.problemsOf(listOf(github), null).single() shouldContain "no regular expression"
    }

    test("watches only HTTPS and loopback addresses") {
        fun problemsOf(url: String): List<String> {
            val document = StandardDeclaration(StandardIds.document("x"), reviewEverySeconds = YEAR, url = url)
            return DeclarationValidator.problemsOf(listOf(document), null)
        }

        problemsOf("https://example.org/spec").shouldBeEmpty()
        problemsOf("http://127.0.0.1:8080/spec").shouldBeEmpty()
        problemsOf("http://example.org/spec") shouldHaveSize 1
        problemsOf("https://user:secret@example.org/") shouldHaveSize 1
        problemsOf("file:///etc/passwd") shouldHaveSize 1
        problemsOf("not a url") shouldHaveSize 1
    }
})
