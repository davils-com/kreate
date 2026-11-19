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
package com.davils.kreate.standard.provider.document

import com.davils.kreate.freshDirectory
import com.davils.kreate.standard.model.StandardDeclaration
import com.davils.kreate.standard.model.StandardIds
import com.davils.kreate.standard.provider.ProviderFixture
import com.davils.kreate.standard.provider.snapshotFor
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe

class DocumentProviderTest : FunSpec({
    test("watches the page of a document by its fingerprint") {
        ProviderFixture(tempdir().freshDirectory()).use { fixture ->
            fixture.text("/din-5008", "DIN 5008:2020-03")
            val url = fixture.server.uri("/din-5008").toString()
            val declaration = StandardDeclaration(StandardIds.din("DIN 5008"), reviewEverySeconds = 1, url = url)
            val provider = DocumentProvider(fixture.context)

            val snapshot = provider.snapshotFor(declaration)

            provider.accepts(declaration) shouldBe true
            provider.accepts(declaration.copy(url = null)) shouldBe false
            snapshot.fingerprint shouldBe Fingerprints.fingerprintOf("DIN 5008:2020-03".toByteArray())
        }
    }
})
