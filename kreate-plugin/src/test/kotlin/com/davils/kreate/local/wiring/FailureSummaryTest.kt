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

package com.davils.kreate.local.wiring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FailureSummaryTest : FunSpec({
    context("firstParagraphOf") {
        test("joins a sentence wrapped over several lines into one line") {
            val message = """
                Local mode was requested with -Pkreate.local=true, but nothing is published to the
                local Maven repository.

                    State directory: /home/user/.gradle/kreate/local
            """.trimIndent()

            firstParagraphOf(message) shouldBe
                "Local mode was requested with -Pkreate.local=true, but nothing is published to the " +
                "local Maven repository."
        }

        test("keeps a single line message as it is") {
            firstParagraphOf("'kreate.local' has to be 'true' or 'false', but was 'yes'.") shouldBe
                "'kreate.local' has to be 'true' or 'false', but was 'yes'."
        }

        test("skips blank lines before the first paragraph") {
            firstParagraphOf("\n\n  Kreate found local development state while running in CI.\n\nDetails") shouldBe
                "Kreate found local development state while running in CI."
        }

        test("an empty message stays empty") {
            firstParagraphOf("") shouldBe ""
        }
    }
})
