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

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private val hostileTexts: Arb<String> = Arb.list(Arb.element(HostileFragments.all), 1..8)
    .map { fragments -> fragments.joinToString("") }

class StandardIdsTest : FunSpec({
    context("a declaration") {
        test("of an RFC is its number") {
            StandardIds.rfc(9562).toString() shouldBe "rfc:9562"
        }

        test("of an ISO standard drops the year and the prefix of the body") {
            StandardIds.iso("ISO 8601-1:2019").toString() shouldBe "iso:8601-1"
            StandardIds.iso("ISO/IEC 27001").toString() shouldBe "iso:iec-27001"
            StandardIds.iso("8601-2") shouldBe StandardIds.iso("ISO 8601-2:2019")
        }

        test("of an ISO standard names the bodies that publish it jointly") {
            StandardIds.iso("ISO 10646").toString() shouldBe "iso:iec-10646"
            StandardIds.iso("ISO-10646:2020") shouldBe StandardIds.iso("ISO/IEC 10646")
            StandardIds.iso("ISO 8859-1").toString() shouldBe "iso:iec-8859-1"
            StandardIds.iso("ISO 42010").toString() shouldBe "iso:iec-ieee-42010"
            StandardIds.iso("ISO/IEEE 11073-10101").toString() shouldBe "iso:ieee-11073-10101"
            StandardIds.iso("ISO 106460").toString() shouldBe "iso:106460"
        }

        test("of an ISO standard gives back its reference") {
            StandardIdGrammar.isoReferenceOf(StandardIds.iso("iso/iec/ieee 42010")) shouldBe "ISO/IEC/IEEE 42010"
            StandardIdGrammar.isoReferenceOf(StandardIds.iso("ISO 8601-1")) shouldBe "ISO 8601-1"
        }

        test("of a test suite is its name") {
            StandardIds.suite("XMLConf").toString() shouldBe "suite:xmlconf"
            StandardIdGrammar.parse("suite:xsdtests") shouldBe StandardIds.suite("xsdtests")
            shouldThrow<IllegalArgumentException> { StandardIds.suite("w3c/xsdtests") }
        }

        test("of a Unicode report accepts the usual spellings") {
            StandardIds.unicode("UAX #31").toString() shouldBe "unicode:uax-31"
            StandardIds.unicode("uts39").toString() shouldBe "unicode:uts-39"
            StandardIds.unicode(null).toString() shouldBe "unicode:ucd"
        }

        test("of other bodies is lower case") {
            StandardIds.w3c("WCAG22").toString() shouldBe "w3c:wcag22"
            StandardIds.whatwg("URL").toString() shouldBe "whatwg:url"
            StandardIds.github("Yaml/yaml-spec").toString() shouldBe "github:yaml/yaml-spec"
            StandardIds.din("DIN EN ISO 9001").toString() shouldBe "din:en-iso-9001"
            StandardIds.document("OASIS SARIF 2.1.0").toString() shouldBe "document:oasis-sarif-2.1.0"
            StandardIds.draft("draft-ietf-httpbis-resumable-upload").name shouldBe "draft-ietf-httpbis-resumable-upload"
        }

        test("refuses what no body names") {
            shouldThrow<IllegalArgumentException> { StandardIds.rfc(0) }
            shouldThrow<IllegalArgumentException> { StandardIds.rfc(100_000) }
            shouldThrow<IllegalArgumentException> { StandardIds.draft("draft-ietf-httpbis-resumable-upload-05") }
            shouldThrow<IllegalArgumentException> { StandardIds.draft("rfc9562") }
            shouldThrow<IllegalArgumentException> { StandardIds.iso("ISO") }
            shouldThrow<IllegalArgumentException> { StandardIds.github("yaml") }
            shouldThrow<IllegalArgumentException> { StandardIds.github("a/b/c") }
            shouldThrow<IllegalArgumentException> { StandardIds.unicode("UAX #0") }
            shouldThrow<IllegalArgumentException> { StandardIds.document("---") }
        }

        test("escapes the text it refuses") {
            checkAll(hostileTexts) { text ->
                val message = try {
                    StandardIds.w3c(text).toString()
                } catch (failure: IllegalArgumentException) {
                    failure.message.orEmpty()
                }

                unsafeCodePointsIn(message).shouldBeEmpty()
            }
        }
    }

    context("a canonical id") {
        test("is read back as written") {
            listOf("rfc:9562", "draft:draft-ietf-x-y", "iso:iec-27001", "w3c:webauthn-3", "whatwg:url",
                "unicode:ucd", "unicode:uax-31", "github:yaml/yaml-spec", "din:5008", "document:ecma-404")
                .forEach { text -> StandardIdGrammar.parse(text)?.toString() shouldBe text }
        }

        test("of any RFC is read back") {
            checkAll(Arb.int(1..MAX_RFC_NUMBER)) { number ->
                val id = StandardIds.rfc(number)

                StandardIdGrammar.parse(id.toString()) shouldBe id
            }
        }

        test("refuses unknown kinds, leading zeros and other text") {
            listOf("", "rfc", "rfc:", "rfc:09562", "ietf:9562", "RFC:9562", "github:yaml", "unicode:uax-31x")
                .forEach { text -> StandardIdGrammar.parse(text).shouldBeNull() }
        }

        test("accepts only text that is safe to show") {
            checkAll(hostileTexts) { text ->
                val parsed = StandardIdGrammar.parse("w3c:$text")

                unsafeCodePointsIn(parsed?.toString().orEmpty()).shouldBeEmpty()
            }
        }
    }

    context("a link") {
        test("points at the page of the body") {
            StandardLinks.infoUrlOf(StandardIds.rfc(9562)) shouldBe "https://www.rfc-editor.org/info/rfc9562"
            StandardLinks.infoUrlOf(StandardIds.unicode("UAX #31")) shouldBe "https://www.unicode.org/reports/tr31/"
            StandardLinks.infoUrlOf(StandardIds.iso("ISO 8601-1")) shouldBe
                "https://www.iso.org/search.html?q=ISO+8601-1"
            StandardLinks.infoUrlOf(StandardIds.din("DIN 5008"), "https://example.org/5008") shouldBe
                "https://example.org/5008"
            StandardLinks.erratumUrlOf(StandardDeclaration(StandardIds.rfc(9562))) shouldBe
                "https://www.rfc-editor.org/errata/rfc9562"
        }

        test("of a W3C specification points at its errata, of a suite at where it comes from") {
            val xml = StandardDeclaration(StandardIds.w3c("xml"))
            val overridden = xml.copy(errata = "https://github.com/w3c/xml/issues")
            val archive = SuiteSource(vendored = "x", archive = "https://www.w3.org/XML/Test/xmlts.tar.gz")
            val repository = SuiteSource(vendored = "x", github = "w3c/xsdtests")
            val watched = archive.copy(watch = "https://www.w3.org/XML/Test/")

            StandardLinks.erratumUrlOf(xml) shouldBe "https://www.w3.org/TR/xml/"
            StandardLinks.erratumUrlOf(overridden) shouldBe "https://github.com/w3c/xml/issues"
            StandardLinks.erratumUrlOf(StandardDeclaration(StandardIds.iso("ISO 8601-1"))).shouldBeNull()
            StandardLinks.infoUrlOf(StandardDeclaration(StandardIds.suite("a"), suite = archive)) shouldBe
                "https://www.w3.org/XML/Test/xmlts.tar.gz"
            StandardLinks.infoUrlOf(StandardDeclaration(StandardIds.suite("a"), suite = repository)) shouldBe
                "https://github.com/w3c/xsdtests"
            StandardLinks.infoUrlOf(StandardDeclaration(StandardIds.suite("a"), suite = watched)) shouldBe
                "https://www.w3.org/XML/Test/"
        }
    }
})
