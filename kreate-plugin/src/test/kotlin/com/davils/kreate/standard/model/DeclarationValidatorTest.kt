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

private const val SHA: String = "9b61db9f5dbffa545f4b8d78422167083a8568c59bd1129f94138f936cf6fc1f"

private const val COMMIT: String = "7bc3365c652a322f3d762021b3879eb92dae7e30"

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

    test("takes an errata address only for a W3C specification, on w3.org or github.com") {
        fun problemsOf(id: StandardId, errata: String): List<String> =
            DeclarationValidator.problemsOf(listOf(StandardDeclaration(id, errata = errata)), null)

        problemsOf(StandardIds.w3c("xml"), "https://www.w3.org/XML/xml-V10-5e-errata").shouldBeEmpty()
        problemsOf(StandardIds.w3c("xml"), "https://github.com/w3c/xml/issues?q=label:erratum").shouldBeEmpty()
        problemsOf(StandardIds.w3c("xml"), "http://www.w3.org/XML/xml-V10-5e-errata").single() shouldContain
            "neither an HTTPS page on w3.org"
        problemsOf(StandardIds.w3c("xml"), "https://example.org/errata") shouldHaveSize 1
        problemsOf(StandardIds.rfc(1), "https://www.w3.org/errata").single() shouldContain "only w3c() does"
    }

    test("refuses a W3C name without a version") {
        val xpath = StandardDeclaration(StandardIds.w3c("xpath"))
        val c14n = StandardDeclaration(StandardIds.w3c("xml-c14n"))

        DeclarationValidator.problemsOf(listOf(xpath), null).single() shouldContain "declare xpath-10 or xpath-31"
        DeclarationValidator.problemsOf(listOf(c14n), null).single() shouldContain "xml-c14n10 or xml-c14n11"
    }

    test("takes a suite from a pinned archive or a pinned commit, vendored inside the project") {
        fun problemsOf(suite: SuiteSource): List<String> =
            DeclarationValidator.problemsOf(listOf(StandardDeclaration(StandardIds.suite("s"), suite = suite)), null)

        val archive = SuiteSource(vendored = "src/xmlconf", archive = "https://a.example/t.tar.gz", sha256 = SHA)
        val github = SuiteSource(vendored = "tests", github = "w3c/xsdtests", commit = COMMIT)

        problemsOf(archive).shouldBeEmpty()
        problemsOf(archive.copy(watch = "https://www.w3.org/XML/Test/", root = "xmlconf")).shouldBeEmpty()
        problemsOf(github).shouldBeEmpty()
        problemsOf(archive.copy(sha256 = "abc")).single() shouldContain "64 hexadecimal digits"
        problemsOf(archive.copy(commit = COMMIT)).single() shouldContain "commit only with a github"
        problemsOf(archive.copy(archive = "http://a.example/t.tar.gz")).single() shouldContain "neither HTTPS"
        problemsOf(archive.copy(github = "w3c/xsdtests")).single() shouldContain "not both"
        problemsOf(SuiteSource(vendored = "x")).single() shouldContain "needs an archive"
        problemsOf(github.copy(commit = "main")).single() shouldContain "40 hexadecimal digits"
        problemsOf(github.copy(github = "xsdtests")).single() shouldContain "not of the form owner/name"
        problemsOf(github.copy(sha256 = SHA)).single() shouldContain "sha256 only with an archive"
        problemsOf(github.copy(vendored = null)).single() shouldContain "needs vendored"
        problemsOf(github.copy(vendored = "../elsewhere")).single() shouldContain "no relative path"
        problemsOf(github.copy(vendored = "/etc")).single() shouldContain "no relative path"
        problemsOf(github.copy(vendored = " ")).single() shouldContain "no relative path"
        problemsOf(github.copy(root = "../x")).single() shouldContain "the root"
        problemsOf(github.copy(watch = "file:///etc/passwd")).single() shouldContain "the watch"
    }

    test("needs a suite for suite() and takes one nowhere else") {
        val bare = StandardDeclaration(StandardIds.suite("s"))
        val misplaced = StandardDeclaration(StandardIds.rfc(1), suite = SuiteSource(vendored = "x"))

        DeclarationValidator.problemsOf(listOf(bare), null).single() shouldContain "needs a suite"
        DeclarationValidator.problemsOf(listOf(misplaced), null).single() shouldContain "only suite() does"
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
