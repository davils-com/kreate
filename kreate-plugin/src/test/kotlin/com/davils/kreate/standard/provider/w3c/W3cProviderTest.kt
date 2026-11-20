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
package com.davils.kreate.standard.provider.w3c

import com.davils.kreate.freshDirectory
import com.davils.kreate.net.LoopbackServer
import com.davils.kreate.net.LoopbackServer.Companion.reply
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.failure
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

private fun specification(status: String): String = """
    {"shortname": "webauthn-2", "title": "Web Authentication: Level 2",
     "_links": {
       "latest-version": {"href": "https://api.w3.org/specifications/webauthn-2/versions/20210408", "title": "$status"},
       "series": {"href": "https://api.w3.org/specification-series/webauthn"}}}
""".trimIndent()

private const val SERIES: String = """
{"shortname": "webauthn", "_links": {"current-specification": {"href": "https://api.w3.org/specifications/webauthn-3"}}}
"""

private fun xml(shortname: String = "xml"): String = """
    {"shortname": "$shortname", "title": "Extensible Markup Language (XML) 1.0 (Fifth Edition)",
     "_links": {
       "latest-version": {
         "href": "https://api.w3.org/specifications/$shortname/versions/20081126", "title": "Recommendation"},
       "series": {"href": "https://api.w3.org/specification-series/xml"}}}
""".trimIndent()

private fun version(errata: String?): String {
    val link = errata?.let { address -> """"errata": "$address",""" }.orEmpty()
    return """{"status": "Recommendation", $link "date": "2008-11-26"}"""
}

private const val XML_SERIES: String = """
{"shortname": "xml", "_links": {"current-specification": {"href": "https://api.w3.org/specifications/xml11"}}}
"""

private const val ERRATA_PAGE: String = """
<html><head><meta name="viewport" content="width=device-width"></head><body>
<h2 id="substantive" data-id="ignored">Substantive errata</h2>
<h3><a name="S01" id="S01">S01</a></h3>
<h3><a name='E01'>E01</a></h3>
<p class="note">No anchor here.</p>
</body></html>
"""

private fun issue(number: Int, state: String, reason: String? = null): String {
    val stateReason = reason?.let { text -> "\"$text\"" } ?: "null"
    return """{"number": $number, "state": "$state", "state_reason": $stateReason}"""
}

class W3cProviderTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach { fixture = ProviderFixture(tempdir().freshDirectory()) }

    afterEach { fixture.close() }

    test("carries the date and maturity of the latest version and a newer level of the series") {
        fixture.json("/specifications/webauthn-2/versions/20210408", version(null))
        fixture.json("/specifications/webauthn-2", specification("Recommendation"))
        fixture.json("/specification-series/webauthn", SERIES)

        val declared = StandardDeclaration(StandardIds.w3c("webauthn-2"))
        val snapshot = W3cProvider(fixture.context).snapshotFor(declared)

        snapshot.title shouldBe "Web Authentication: Level 2"
        snapshot.status shouldBe "Recommendation"
        snapshot.revision shouldBe "2021-04-08"
        snapshot.withdrawn shouldBe false
        snapshot.updatedBy shouldBe listOf("w3c:webauthn-3")
        snapshot.errata.shouldBeEmpty()
        snapshot.fingerprint.shouldBeNull()
    }

    test("counts a superseded specification as withdrawn") {
        fixture.json("/specifications/webauthn-2/versions/20210408", version(null))
        fixture.json("/specifications/webauthn-2", specification("Superseded Recommendation"))
        fixture.json("/specification-series/webauthn", SERIES.replace("webauthn-3", "webauthn-2"))

        val declared = StandardDeclaration(StandardIds.w3c("webauthn-2"))
        val snapshot = W3cProvider(fixture.context).snapshotFor(declared)

        snapshot.withdrawn shouldBe true
        snapshot.updatedBy.shouldBeEmpty()
    }

    context("the errata") {
        beforeEach {
            fixture.json("/specifications/xml", xml())
            fixture.json("/specification-series/xml", XML_SERIES)
        }

        test("are the anchors of the page the latest version links, together with its fingerprint") {
            fixture.json("/specifications/xml/versions/20081126", version("http://www.w3.org/XML/xml-V10-5e-errata"))
            fixture.text("/XML/xml-V10-5e-errata", ERRATA_PAGE)

            val snapshot = W3cProvider(fixture.context).snapshotFor(StandardDeclaration(StandardIds.w3c("xml")))

            snapshot.errata shouldBe mapOf(
                "substantive" to ErratumState.VERIFIED,
                "S01" to ErratumState.VERIFIED,
                "E01" to ErratumState.VERIFIED
            )
            snapshot.fingerprint.orEmpty() shouldStartWith "sha256:"
        }

        test("are read from the issues of a GitHub repository, open, completed or not planned") {
            val requests = mutableListOf<String>()
            fixture.server.handle("/search/issues") { exchange ->
                val query = exchange.requestURI.rawQuery.orEmpty()
                requests += query
                val first = (1..100).map { number -> issue(number, "open") }
                val second = listOf(issue(101, "closed", "completed"), issue(102, "closed", "not_planned"))
                val pages = mapOf("1" to first, "2" to second + issue(103, "closed"))
                val items = pages.getValue(query.substringAfterLast("&page=")).joinToString(",")
                val body = """{"total_count": 103, "incomplete_results": false, "items": [$items]}"""
                exchange.reply(LoopbackServer.HTTP_OK, mapOf("Content-Type" to "application/json"), body.toByteArray())
            }
            val errata = "https://github.com/w3c/xml/issues?q=is%3Aopen+label%3Aerratum"
            val declared = StandardDeclaration(StandardIds.w3c("xml"), errata = errata)

            val snapshot = W3cProvider(fixture.context).snapshotFor(declared)

            snapshot.errata.size shouldBe 103
            snapshot.errata["#1"] shouldBe ErratumState.REPORTED
            snapshot.errata["#101"] shouldBe ErratumState.VERIFIED
            snapshot.errata["#102"] shouldBe ErratumState.REJECTED
            snapshot.errata["#103"] shouldBe ErratumState.VERIFIED
            snapshot.fingerprint.shouldBeNull()
            requests.size shouldBe 2
            requests.first() shouldContain "q=repo%3Aw3c%2Fxml+is%3Aissue+label%3Aerratum"
            fixture.server.requests.none { request -> request.path.contains("/versions/") } shouldBe true
        }

        test("are not read from a host other than w3.org or github.com") {
            fixture.json("/specifications/xml/versions/20081126", version("https://w3c.github.io/xml/errata.html"))

            val snapshot = W3cProvider(fixture.context).snapshotFor(StandardDeclaration(StandardIds.w3c("xml")))

            snapshot.errata.shouldBeEmpty()
            snapshot.fingerprint.shouldBeNull()
        }

        test("make the specification unknown when the page does not exist") {
            fixture.json("/specifications/xml/versions/20081126", version("https://www.w3.org/XML/missing-errata"))

            val outcome = W3cProvider(fixture.context).snapshotOf(StandardDeclaration(StandardIds.w3c("xml")))

            outcome.failure().shouldBeInstanceOf<LookupFailure.Unknown>()
        }
    }

    context("a newer level of the series") {
        beforeEach {
            fixture.json("/specifications/xml/versions/20081126", version(null))
            fixture.json("/specifications/xml", xml())
            fixture.json("/specification-series/xml", XML_SERIES)
        }

        test("is no update when the build declares it too") {
            val declarations = listOf("xml", "xml11").map { name -> StandardDeclaration(StandardIds.w3c(name)) }

            val snapshot = W3cProvider(fixture.context, declarations).snapshotFor(declarations.first())

            snapshot.updatedBy.shouldBeEmpty()
        }

        test("is an update when the build does not declare it") {
            val declarations = listOf("xml", "xml-names").map { name -> StandardDeclaration(StandardIds.w3c(name)) }

            val snapshot = W3cProvider(fixture.context, declarations).snapshotFor(declarations.first())

            snapshot.updatedBy shouldBe listOf("w3c:xml11")
        }
    }
})
