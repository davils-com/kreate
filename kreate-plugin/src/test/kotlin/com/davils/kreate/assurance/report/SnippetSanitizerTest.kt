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
package com.davils.kreate.assurance.report

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith

class SnippetSanitizerTest : FunSpec({
    context("escaping") {
        test("keeps ordinary text and tabs") {
            SnippetSanitizer.escape("val a\t= 1") shouldBe "val a\t= 1"
        }

        mapOf(
            "a right-to-left override" to ("\u202E" to "\\u{202E}"),
            "a zero width space" to ("\u200B" to "\\u{200B}"),
            "an escape character" to ("\u001B" to "\\u{001B}"),
            "a line break" to ("\n" to "\\u{000A}"),
            "a tag character" to (String(Character.toChars(0xE0041)) to "\\u{E0041}")
        ).forEach { (name, case) ->
            test("makes $name visible") {
                SnippetSanitizer.escape(case.first) shouldBe case.second
            }
        }

        test("leaves an emoji alone") {
            val emoji = String(Character.toChars(0x1F600))

            SnippetSanitizer.escape(emoji) shouldBe emoji
        }
    }

    context("snippets") {
        test("are cut to a readable length") {
            SnippetSanitizer.sanitize("a".repeat(200)) shouldEndWith "a\u2026"
        }

        test("are trimmed") {
            SnippetSanitizer.sanitize("  code  ") shouldBe "code"
        }
    }
})
