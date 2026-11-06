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

package com.davils.kreate.tooling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class BoundedOutputStreamTest : FunSpec({
    test("keeps everything below the limit") {
        val stream = BoundedOutputStream(4)

        stream.write(byteArrayOf(1, 2))
        stream.write(3)

        val output = stream.snapshot()
        output.bytes shouldBe byteArrayOf(1, 2, 3)
        output.isTruncated.shouldBeFalse()
    }

    test("is not truncated at exactly the limit") {
        val stream = BoundedOutputStream(2)

        stream.write(byteArrayOf(1, 2))

        stream.snapshot().isTruncated.shouldBeFalse()
    }

    test("keeps the head of an array that crosses the limit") {
        val stream = BoundedOutputStream(2)

        stream.write(byteArrayOf(1, 2, 3))

        val output = stream.snapshot()
        output.bytes shouldBe byteArrayOf(1, 2)
        output.isTruncated.shouldBeTrue()
    }

    test("drops single bytes beyond the limit") {
        val stream = BoundedOutputStream(1)

        stream.write(1)
        stream.write(2)

        val output = stream.snapshot()
        output.bytes shouldBe byteArrayOf(1)
        output.isTruncated.shouldBeTrue()
    }

    test("drops arrays once it is full") {
        val stream = BoundedOutputStream(1)

        stream.write(1)
        stream.write(byteArrayOf(2, 3))

        stream.snapshot().bytes shouldBe byteArrayOf(1)
    }

    test("decodes its output as UTF-8") {
        val stream = BoundedOutputStream(16)

        stream.write("\u00E9".toByteArray())

        stream.snapshot().text shouldBe "\u00E9"
    }
})
