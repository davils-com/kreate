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

package com.davils.kreate.archive

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LittleEndianBytesTest : FunSpec({
    val bytes = LittleEndianBytes(byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte()))

    test("reads unsigned values with the low byte first") {
        bytes.uint8(3) shouldBe 0xFF
        bytes.uint16(0) shouldBe 0x0201
        bytes.uint32(0) shouldBe 0xFF030201L
    }

    test("copies a slice") {
        bytes.slice(1, 2) shouldBe byteArrayOf(0x02, 0x03)
    }

    test("refuses a read past either end") {
        shouldThrow<ArchiveException.Malformed> { bytes.uint8(4) }
        shouldThrow<ArchiveException.Malformed> { bytes.uint8(-1) }
        shouldThrow<ArchiveException.Malformed> { bytes.uint32(1) }
    }

    test("refuses a slice past either end") {
        shouldThrow<ArchiveException.Malformed> { bytes.slice(3, 2) }
        shouldThrow<ArchiveException.Malformed> { bytes.slice(-1, 2) }
    }
})
