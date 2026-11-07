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
package com.davils.kreate.provenance.typosquat

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EditDistanceTest : FunSpec({
    context("the edit distance") {
        test("is zero for equal names") {
            boundedEditDistance("guava", "guava", limit = 2) shouldBe 0
        }

        test("counts a substitution, an insertion and a deletion") {
            boundedEditDistance("guava", "gauva", limit = 2) shouldBe 2
            boundedEditDistance("guava", "guavaa", limit = 2) shouldBe 1
            boundedEditDistance("guava", "gava", limit = 2) shouldBe 1
        }

        test("stops just above the limit") {
            boundedEditDistance("guava", "jackson", limit = 2) shouldBe 3
            boundedEditDistance("a", "abcdef", limit = 2) shouldBe 3
        }
    }
})
