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

package com.davils.kreate.api.metadata

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KotlinMetadataValuesTest : FunSpec({
    fun values(): KotlinMetadataValues = KotlinMetadataValues(
        kind = 1,
        metadataVersion = intArrayOf(2, 1, 0),
        data1 = arrayOf("data"),
        data2 = arrayOf("strings"),
        extraString = "",
        packageName = "com.acme",
        extraInt = 0
    )

    context("KotlinMetadataValues") {
        test("compares the array contents rather than the array identities") {
            values() shouldBe values()
        }

        test("hashes equal values alike") {
            values().hashCode() shouldBe values().hashCode()
        }

        test("is equal to itself") {
            val same = values()

            same.equals(same) shouldBe true
        }

        test("is not equal to something that is not metadata") {
            values().equals("metadata") shouldBe false
        }

        test("tells values apart by any single field") {
            val original = values()

            original shouldNotBe original.copy(kind = 2)
            original shouldNotBe original.copy(metadataVersion = intArrayOf(1, 9, 0))
            original shouldNotBe original.copy(data1 = arrayOf("other"))
            original shouldNotBe original.copy(data2 = arrayOf("other"))
            original shouldNotBe original.copy(extraString = "facade")
            original shouldNotBe original.copy(packageName = "com.other")
            original shouldNotBe original.copy(extraInt = 1)
        }
    }
})
