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

package com.davils.kreate.digest

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream

private const val ABC_DIGEST: String = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
private const val EMPTY_DIGEST: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

class Sha256Test : FunSpec({
    test("matches the test vectors of FIPS 180-4 for text, bytes, streams and files") {
        val file = tempfile()
        file.writeText("abc")

        Sha256.hexOf("abc") shouldBe ABC_DIGEST
        Sha256.hexOf("abc".toByteArray()) shouldBe ABC_DIGEST
        Sha256.hexOf(ByteArrayInputStream("abc".toByteArray())) shouldBe ABC_DIGEST
        Sha256.hexOf(file.toPath()) shouldBe ABC_DIGEST
        Sha256.hexOf("") shouldBe EMPTY_DIGEST
    }

    test("hands out a fresh digest each time, so one in use never leaks into the next") {
        val used = Sha256.digest()
        used.update("abc".toByteArray())

        Sha256.digest().digest() shouldBe Sha256.digestOf(ByteArray(0))
        Sha256.hexOf(ByteArray(0)) shouldBe EMPTY_DIGEST
    }
})
