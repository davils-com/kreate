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
package com.davils.kreate.standard.provider.github

import com.davils.kreate.freshDirectory
import com.davils.kreate.standard.model.LookupFailure
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.failure
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val RELEASES: String = """
[{"tag_name": "v1.10.0-rc.1", "draft": false, "prerelease": true, "published_at": "2026-11-01T10:00:00Z"},
 {"tag_name": "v1.9.2", "draft": false, "prerelease": false, "published_at": "2026-10-13T23:37:11Z"},
 {"tag_name": "v0.9.0", "draft": false, "prerelease": false, "published_at": "2020-01-01T00:00:00Z"}]
"""

private const val TAGS: String = """[{"name": "master-2020"}, {"name": "1.2.2"}, {"name": "1.2.1"}]"""

private const val DRAFTS: String = """
[{"name": "2025-08.bs", "type": "file"}, {"name": "2026-08.bs", "type": "file"}, {"name": "2026-02.bs", "type": "file"}]
"""

class GitHubProvidersTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach { fixture = ProviderFixture(tempdir().freshDirectory(), token = "secret") }

    afterEach { fixture.close() }

    context("a repository") {
        test("is at its latest final release") {
            fixture.json("/repos/ossf/osv-schema/releases", RELEASES)

            val declared = StandardDeclaration(StandardIds.github("ossf/osv-schema"))
            val snapshot = GitHubProvider(fixture.context).snapshotFor(declared)

            snapshot.revision shouldBe "v1.9.2"
            snapshot.published shouldBe "2026-10-13"
        }

        test("follows only the releases whose tags match") {
            fixture.json("/repos/ossf/osv-schema/releases", RELEASES)
            val declaration = StandardDeclaration(StandardIds.github("ossf/osv-schema"), tags = "^v0\\.")

            GitHubProvider(fixture.context).snapshotFor(declaration).revision shouldBe "v0.9.0"
        }

        test("without releases is at its newest version tag") {
            fixture.json("/repos/yaml/yaml-spec/releases", "[]")
            fixture.json("/repos/yaml/yaml-spec/tags", TAGS)

            val declared = StandardDeclaration(StandardIds.github("yaml/yaml-spec"))
            val snapshot = GitHubProvider(fixture.context).snapshotFor(declared)

            snapshot.revision shouldBe "1.2.2"
        }

        test("that does not exist is unknown") {
            val outcome = GitHubProvider(fixture.context).snapshotOf(StandardDeclaration(StandardIds.github("a/b")))

            outcome.failure().shouldBeInstanceOf<LookupFailure.Unknown>()
        }

        test("never sends a token to a service that is not reached over HTTPS") {
            fixture.json("/repos/ossf/osv-schema/releases", RELEASES)

            val declaration = StandardDeclaration(StandardIds.github("ossf/osv-schema"))

            GitHubProvider(fixture.context).snapshotOf(declaration)

            val request = fixture.server.requests.single()

            request.header("Authorization").shouldBeNull()
        }
    }

    context("a WHATWG standard") {
        test("is at its latest review draft") {
            fixture.json("/repos/whatwg/url/contents/review-drafts", DRAFTS)

            val declared = StandardDeclaration(StandardIds.whatwg("url"))
            val snapshot = WhatwgProvider(fixture.context).snapshotFor(declared)

            snapshot.revision shouldBe "2026-08"
        }
    }
})
