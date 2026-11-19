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
package com.davils.kreate.standard.provider.ietf

import com.davils.kreate.freshDirectory
import com.davils.kreate.standard.model.ErratumState
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.failure
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val RFC_7386: String = """
{"doc_id": "RFC7386", "title": "JSON Merge Patch", "status": "PROPOSED STANDARD", "pub_date": "October 2014",
 "obsoletes": [], "obsoleted_by": ["RFC7396"], "updates": [], "updated_by": [],
 "errata_url": "https://www.rfc-editor.org/errata/rfc7386"}
"""

private const val ERRATA: String = """
[{"errata_id": "4132", "doc-id": "RFC7386", "errata_status_code": "Verified", "errata_type_code": "Technical"},
 {"errata_id": "4133", "doc-id": "RFC7386", "errata_status_code": "Held for Document Update"},
 {"errata_id": "9", "doc-id": "RFC1", "errata_status_code": "Reported"}]
"""

private const val DRAFT: String = """
{"name": "draft-ietf-jsonpath-base", "rev": "21", "pages": 49, "time": "2024-02-22 05:00:18",
 "title": "JSONPath: Query expressions for JSON", "state": "RFC", "rfc": "9535"}
"""

class IetfProvidersTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach { fixture = ProviderFixture(tempdir().freshDirectory()) }

    afterEach { fixture.close() }

    context("an RFC") {
        test("carries its status, its replacements and its errata") {
            fixture.json("/rfc/rfc7386.json", RFC_7386)
            fixture.json("/api/v1/errata.json", ERRATA)

            val declared = StandardDeclaration(StandardIds.rfc(7386))
            val snapshot = RfcProvider(fixture.context).snapshotFor(declared)

            snapshot.title shouldBe "JSON Merge Patch"
            snapshot.status shouldBe "PROPOSED STANDARD"
            snapshot.replacedBy shouldBe listOf("RFC7396")
            snapshot.withdrawn shouldBe true
            snapshot.errata shouldBe mapOf("4132" to ErratumState.VERIFIED, "4133" to ErratumState.HELD)
        }

        test("reads the errata once for every RFC") {
            fixture.json("/rfc/rfc7386.json", RFC_7386)
            fixture.json("/rfc/rfc1.json", RFC_7386)
            fixture.json("/api/v1/errata.json", ERRATA)
            val provider = RfcProvider(fixture.context)

            provider.snapshotOf(StandardDeclaration(StandardIds.rfc(7386)))
            provider.snapshotOf(StandardDeclaration(StandardIds.rfc(1)))

            fixture.server.requests.count { request -> request.path == "/api/v1/errata.json" } shouldBe 1
        }

        test("that the RFC Editor does not know is unknown") {
            val outcome = RfcProvider(fixture.context).snapshotOf(StandardDeclaration(StandardIds.rfc(99_999)))

            outcome.failure().shouldBeInstanceOf<LookupFailure.Unknown>()
        }

        test("without readable errata is unavailable") {
            fixture.json("/rfc/rfc7386.json", RFC_7386)
            fixture.json("/api/v1/errata.json", "{not json")

            val outcome = RfcProvider(fixture.context).snapshotOf(StandardDeclaration(StandardIds.rfc(7386)))

            outcome.failure().shouldBeInstanceOf<LookupFailure.Unavailable>()
        }
    }

    context("an Internet-Draft") {
        test("carries its revision and the RFC it became") {
            fixture.json("/doc/draft-ietf-jsonpath-base/doc.json", DRAFT)
            val declaration = StandardDeclaration(StandardIds.draft("draft-ietf-jsonpath-base"))

            val snapshot = DraftProvider(fixture.context).snapshotFor(declaration)

            snapshot.revision shouldBe "21"
            snapshot.status shouldBe "RFC"
            snapshot.published shouldBe "2024-02-22"
            snapshot.withdrawn shouldBe true
            snapshot.replacedBy shouldBe listOf("RFC9535")
        }
    }
})
