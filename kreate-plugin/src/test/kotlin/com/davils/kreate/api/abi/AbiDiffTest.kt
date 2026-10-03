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

package com.davils.kreate.api.abi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class AbiDiffTest : FunSpec({
    fun dump(vararg lines: String): String = lines.joinToString(separator = "\n", postfix = "\n")

    context("ABI diff") {
        test("reports nothing for identical dumps") {
            AbiDiff.render(dump("same"), dump("same")) shouldBe null
        }

        test("marks an added line and leaves the shared context out") {
            val diff = checkNotNull(
                AbiDiff.render(
                    expected = dump("class A {", "\tfun a ()V", "}"),
                    actual = dump("class A {", "\tfun a ()V", "\tfun b ()V", "}")
                )
            )

            diff shouldContain "+\tfun b ()V"
            diff shouldNotContain "fun a ()V"
        }

        test("marks a removed line") {
            val diff = checkNotNull(
                AbiDiff.render(
                    expected = dump("class A {", "\tfun a ()V", "\tfun b ()V", "}"),
                    actual = dump("class A {", "\tfun a ()V", "}")
                )
            )

            diff shouldContain "-\tfun b ()V"
        }

        test("caps a very large difference instead of printing the whole dump") {
            val actual = dump(*Array(200) { "line $it" })

            val diff = checkNotNull(AbiDiff.render(expected = "", actual = actual))

            diff shouldContain "more line(s)"
        }
    }
})
