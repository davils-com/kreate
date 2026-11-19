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
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private fun specification(status: String): String = """
    {"shortname": "webauthn-2", "title": "Web Authentication: Level 2",
     "_links": {
       "latest-version": {"href": "https://api.w3.org/specifications/webauthn-2/versions/20210408", "title": "$status"},
       "series": {"href": "https://api.w3.org/specification-series/webauthn"}}}
""".trimIndent()

private const val SERIES: String = """
{"shortname": "webauthn", "_links": {"current-specification": {"href": "https://api.w3.org/specifications/webauthn-3"}}}
"""

class W3cProviderTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach { fixture = ProviderFixture(tempdir().freshDirectory()) }

    afterEach { fixture.close() }

    test("carries the date and maturity of the latest version and a newer level of the series") {
        fixture.json("/specifications/webauthn-2", specification("Recommendation"))
        fixture.json("/specification-series/webauthn", SERIES)

        val declared = StandardDeclaration(StandardIds.w3c("webauthn-2"))
        val snapshot = W3cProvider(fixture.context).snapshotFor(declared)

        snapshot.title shouldBe "Web Authentication: Level 2"
        snapshot.status shouldBe "Recommendation"
        snapshot.revision shouldBe "2021-04-08"
        snapshot.withdrawn shouldBe false
        snapshot.updatedBy shouldBe listOf("w3c:webauthn-3")
    }

    test("counts a superseded specification as withdrawn") {
        fixture.json("/specifications/webauthn-2", specification("Superseded Recommendation"))
        fixture.json("/specification-series/webauthn", SERIES.replace("webauthn-3", "webauthn-2"))

        val declared = StandardDeclaration(StandardIds.w3c("webauthn-2"))
        val snapshot = W3cProvider(fixture.context).snapshotFor(declared)

        snapshot.withdrawn shouldBe true
        snapshot.updatedBy.shouldBeEmpty()
    }
})
