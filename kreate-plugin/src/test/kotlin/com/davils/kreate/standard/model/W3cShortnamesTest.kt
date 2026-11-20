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
import io.kotest.matchers.shouldBe

class W3cShortnamesTest : FunSpec({
    test("maps the old addresses of a specification to its short name") {
        W3cShortnames.linked("REC-xml").toString() shouldBe "w3c:xml"
        W3cShortnames.linked("REC-xml-names").toString() shouldBe "w3c:xml-names"
        W3cShortnames.linked("xml-c14n").toString() shouldBe "w3c:xml-c14n10"
        W3cShortnames.linked("xml-c14n11").toString() shouldBe "w3c:xml-c14n11"
    }

    test("drops the maturity and the date of a dated version") {
        W3cShortnames.linked("REC-xml11-20060816").toString() shouldBe "w3c:xml11"
        W3cShortnames.linked("REC-xml-c14n-20010315").toString() shouldBe "w3c:xml-c14n10"
        W3cShortnames.linked("REC-xml-names-20091208").toString() shouldBe "w3c:xml-names"
    }

    test("keeps the name of a specification that has no alias") {
        W3cShortnames.linked("webauthn-3").toString() shouldBe "w3c:webauthn-3"
        W3cShortnames.linked("xpath").toString() shouldBe "w3c:xpath"
    }

    test("knows the versions of a name the W3C sends to its latest version") {
        W3cShortnames.versionsOf(StandardIds.w3c("xpath")).map { id -> id.name } shouldContainExactly
            listOf("xpath-10", "xpath-31")
        W3cShortnames.versionsOf(StandardIds.w3c("xml-c14n")).map { id -> id.name } shouldContainExactly
            listOf("xml-c14n10", "xml-c14n11")
        W3cShortnames.versionsOf(StandardIds.w3c("xpath-10")).shouldBeEmpty()
        W3cShortnames.versionsOf(StandardIds.document("xpath")).shouldBeEmpty()
    }
})
