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
package com.davils.kreate.standard.provider.unicode

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

private const val README: String = """
# Unicode Standard Data Files & Charts

This directory contains final data files and charts for version 18.0.0 of the
Unicode Character Database and for the synchronized Unicode Technical Standards.
"""

private const val REPORT: String = """
<html><head><title>UAX #31: Unicode Identifiers and Syntax</title></head><body>
<table class="simple">
  <tr><td>Version</td><td>Unicode 18.0.0</td></tr>
  <tr><td>Date</td>
      <td>2026-09-01</td></tr>
  <tr><td>This Version</td><td><a href="https://www.unicode.org/reports/tr31/tr31-45.html">tr31-45</a></td></tr>
  <tr><td>Previous Version</td><td><a href="https://www.unicode.org/reports/tr31/tr31-43.html">tr31-43</a></td></tr>
</table></body></html>
"""

class UnicodeProviderTest : FunSpec({
    lateinit var fixture: ProviderFixture

    beforeEach { fixture = ProviderFixture(tempdir().freshDirectory()) }

    afterEach { fixture.close() }

    test("reads the version of the Unicode Character Database") {
        fixture.text("/Public/UCD/latest/ReadMe.txt", README)

        val declared = StandardDeclaration(StandardIds.unicode(null))
        val snapshot = UnicodeProvider(fixture.context).snapshotFor(declared)

        snapshot.revision shouldBe "18.0.0"
    }

    test("reads the revision, title and date of a report") {
        fixture.text("/reports/tr31/", REPORT)

        val declared = StandardDeclaration(StandardIds.unicode("UAX #31"))
        val snapshot = UnicodeProvider(fixture.context).snapshotFor(declared)

        snapshot.revision shouldBe "45"
        snapshot.title shouldBe "UAX #31: Unicode Identifiers and Syntax"
        snapshot.published shouldBe "2026-09-01"
    }

    test("is unavailable when a page no longer says its version") {
        fixture.text("/reports/tr31/", "<html>moved</html>")

        val outcome = UnicodeProvider(fixture.context).snapshotOf(StandardDeclaration(StandardIds.unicode("UAX #31")))

        outcome.failure().shouldBeInstanceOf<LookupFailure.Unavailable>()
    }
})
