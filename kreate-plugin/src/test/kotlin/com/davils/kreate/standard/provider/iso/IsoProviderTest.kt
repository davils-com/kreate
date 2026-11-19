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
package com.davils.kreate.standard.provider.iso

import com.davils.kreate.freshDirectory
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

private const val EXPORT_PATH: String = "/iso_deliverables_metadata/json/iso_deliverables_metadata.jsonl"

private val EXPORT: String = listOf(
    """{"id":2,"reference":"ISO/WD 0","title":{"en":"Road vehicles"},"publicationDate":null,"currentStage":2098}""",
    """{"id":42103,"reference":"ISO/IEC 27001:2005","title":{"en":"ISMS"},"publicationDate":"2005-10-14",""" +
        """"currentStage":9599,"replacedBy":[54534]}""",
    """{"id":54534,"reference":"ISO/IEC 27001:2013","title":{"en":"ISMS"},"publicationDate":"2013-09-25",""" +
        """"currentStage":9599,"replacedBy":[82875]}""",
    """{"id":82875,"reference":"ISO/IEC 27001:2022","title":{"en":"Information security management systems"},""" +
        """"publicationDate":"2022-10-25","currentStage":6060,"replacedBy":null}""",
    """{"id":88435,"reference":"ISO/IEC 27001:2022/Amd 1:2024","title":{"en":"Climate action changes"},""" +
        """"publicationDate":"2024-02-23","currentStage":6060}""",
    """{"id":82873,"reference":"ISO/IEC 27001:2013/DAmd 1","title":{"en":"Draft"},"currentStage":4098}""",
    """{"id":70907,"reference":"ISO 8601-1:2019","title":{"en":"Date and time"},"publicationDate":"2019-02-25",""" +
        """"currentStage":9092,"replacedBy":[90784]}""",
    """{"id":40874,"reference":"ISO 8601:2004","title":{"en":"Data elements"},"publicationDate":"2004-12-01",""" +
        """"currentStage":9599,"replacedBy":[70907,70908]}""",
    "not json at all"
).joinToString("\n")

class IsoProviderTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach {
        fixture = ProviderFixture(tempdir().freshDirectory())
        fixture.text(EXPORT_PATH, EXPORT)
    }

    afterEach { fixture.close() }

    fun providerOf(vararg references: String): IsoProvider {
        val declarations = references.map { reference -> StandardDeclaration(StandardIds.iso(reference)) }
        return IsoProvider(fixture.context, declarations)
    }

    test("is at the valid edition with its supplements") {
        val declared = StandardDeclaration(StandardIds.iso("ISO/IEC 27001"))
        val snapshot = providerOf("ISO/IEC 27001").snapshotFor(declared)

        snapshot.revision shouldBe "ISO/IEC 27001:2022"
        snapshot.status shouldBe "60.60"
        snapshot.title shouldBe "Information security management systems"
        snapshot.updatedBy shouldBe listOf("ISO/IEC 27001:2022/Amd 1:2024")
        snapshot.withdrawn shouldBe false
    }

    test("tells a standard that is to be revised by its stage") {
        val declared = StandardDeclaration(StandardIds.iso("ISO 8601-1"))
        val snapshot = providerOf("ISO 8601-1").snapshotFor(declared)

        snapshot.status shouldBe "90.92"
        snapshot.withdrawn shouldBe false
        snapshot.replacedBy shouldBe emptyList()
    }

    test("counts a standard without a valid edition as withdrawn") {
        val declared = StandardDeclaration(StandardIds.iso("ISO 8601"))
        val snapshot = providerOf("ISO 8601").snapshotFor(declared)

        snapshot.withdrawn shouldBe true
        snapshot.replacedBy shouldBe listOf("ISO deliverable 70907", "ISO deliverable 70908")
    }

    test("downloads the export once for every standard") {
        val provider = providerOf("ISO 8601-1", "ISO/IEC 27001")

        provider.snapshotOf(StandardDeclaration(StandardIds.iso("ISO 8601-1")))
        provider.snapshotOf(StandardDeclaration(StandardIds.iso("ISO/IEC 27001")))

        fixture.server.requests.size shouldBe 1
    }

    test("does not know a standard ISO does not publish") {
        val outcome = providerOf("ISO 99999").snapshotOf(StandardDeclaration(StandardIds.iso("ISO 99999")))

        outcome.failure().shouldBeInstanceOf<LookupFailure.Unknown>()
    }
})
