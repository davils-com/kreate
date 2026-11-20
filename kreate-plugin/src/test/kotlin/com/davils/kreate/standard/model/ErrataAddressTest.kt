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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private fun issuesOf(url: String): ErrataAddress.Issues = ErrataAddress.parse(url).shouldBeInstanceOf()

class ErrataAddressTest : FunSpec({
    test("reads a page of the W3C with its query") {
        val page = ErrataAddress.parse("https://www.w3.org/XML/errata").shouldBeInstanceOf<ErrataAddress.Page>()
        val queried = ErrataAddress.parse("https://w3.org/errata?spec=xml").shouldBeInstanceOf<ErrataAddress.Page>()

        page.path shouldBe "/XML/errata"
        queried.path shouldBe "/errata?spec=xml"
    }

    test("asks GitHub for every issue of the repository that matches the query, open or closed") {
        val queried = issuesOf("https://github.com/W3C/xmlschema/issues?q=is%3Aopen+label%3Aerratum+repo%3Aother%2Fx")
        val labelled = issuesOf("https://github.com/w3c/webauthn/labels/errata")
        val all = issuesOf("https://github.com/w3c/webauthn/issues")

        queried.query shouldBe "repo:w3c/xmlschema is:issue label:erratum"
        labelled.query shouldBe "repo:w3c/webauthn is:issue label:\"errata\""
        all.query shouldBe "repo:w3c/webauthn is:issue"
    }

    test("upgrades plain HTTP only where asked to") {
        ErrataAddress.parse("http://www.w3.org/2001/03/C14N-errata").shouldBeNull()
        ErrataAddress.parse("http://www.w3.org/2001/03/C14N-errata", upgradesPlainHttp = true)
            .shouldBeInstanceOf<ErrataAddress.Page>()
    }

    test("refuses other hosts, credentials, ports and other pages of GitHub") {
        listOf(
            "https://w3c.github.io/xml/errata.html",
            "https://evil.example/www.w3.org/errata",
            "https://user:secret@www.w3.org/errata",
            "https://www.w3.org:8443/errata",
            "https://github.com/w3c/xml/pulls",
            "https://github.com/w3c",
            "https://github.com/w3c/xml/issues/12",
            "ftp://www.w3.org/errata",
            "not a url"
        ).forEach { url -> ErrataAddress.parse(url, upgradesPlainHttp = true).shouldBeNull() }
    }
})
