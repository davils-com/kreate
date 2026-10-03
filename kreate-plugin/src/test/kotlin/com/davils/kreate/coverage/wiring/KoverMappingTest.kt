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

package com.davils.kreate.coverage.wiring

import com.davils.kreate.coverage.Aggregation
import com.davils.kreate.coverage.CoverageUnit
import com.davils.kreate.coverage.Grouping
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KoverMappingTest : FunSpec({
    context("mapping onto Kover") {
        test("every coverage unit maps to the Kover unit of the same name") {
            CoverageUnit.entries.forEach { unit ->
                val kover = unit.toKover()

                kover.name shouldBe unit.name
            }
        }

        test("every aggregation maps to the Kover aggregation of the same name") {
            Aggregation.entries.forEach { aggregation ->
                val kover = aggregation.toKover()

                kover.name shouldBe aggregation.name
            }
        }

        test("every grouping maps to the Kover grouping of the same name") {
            Grouping.entries.forEach { grouping ->
                val kover = grouping.toKover()

                kover.name shouldBe grouping.name
            }
        }
    }
})
