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
package com.davils.kreate.provenance

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class BoundedParallelTest : FunSpec({
    context("a bounded map") {
        test("keeps the order of its items") {
            val items = (1..50).toList()

            boundedMap(items, threads = 8) { item -> item * 2 } shouldBe items.map { item -> item * 2 }
        }

        test("does nothing for no items") {
            boundedMap(emptyList<Int>(), threads = 8) { item -> item }.shouldBeEmpty()
        }

        test("passes on the failure of an item as it is") {
            val failure = shouldThrow<IllegalStateException> {
                boundedMap(listOf(1, 2, 3), threads = 2) { item -> check(item != 2) { "item $item" } }
            }

            failure.message shouldBe "item 2"
        }
    }
})
