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
package com.davils.kreate.governance.catalog

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TextDiffTest : FunSpec({
    context("a diff") {
        test("is empty for equal texts") {
            TextDiff.unified("a.toml", "x\ny", "x\ny") shouldBe ""
        }

        test("names the changed and the added lines") {
            val diff = TextDiff.unified("a.toml", "a\nb\nc", "a\nB\nc\nd")

            val expected = listOf("--- a/a.toml", "+++ b/a.toml", "@@ -2 @@", "-b", "@@ +2 @@", "+B", "@@ +4 @@", "+d")
            diff shouldBe expected.joinToString("\n", postfix = "\n")
        }
    }
})
