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

package com.davils.kreate.license.header

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

private const val NOTICE: String = "Copyright 2026 Example\n\nAll rights reserved."

class LicenseHeaderTest : FunSpec({
    val header = LicenseHeader(NOTICE)

    context("the comment style") {
        test("is a block comment for sources and build scripts") {
            listOf("kt", "kts", "java", "KT").forEach { extension ->
                CommentStyle.forExtension(extension) shouldBe CommentStyle.BLOCK
            }
        }

        test("is a hash comment for properties, YAML, TOML and shell files") {
            listOf("properties", "yaml", "yml", "toml", "sh").forEach { extension ->
                CommentStyle.forExtension(extension) shouldBe CommentStyle.HASH
            }
        }
    }

    context("the rendered header") {
        test("is a block comment without trailing blanks") {
            header.render(CommentStyle.BLOCK) shouldBe
                "/*\n * Copyright 2026 Example\n *\n * All rights reserved.\n */\n"
        }

        test("is a run of hash lines without trailing blanks") {
            header.render(CommentStyle.HASH) shouldBe "#\n# Copyright 2026 Example\n#\n# All rights reserved.\n#\n"
        }

        test("ignores the indentation and surrounding line breaks of the notice") {
            val indented = LicenseHeader("\n    Copyright 2026 Example\n\n    All rights reserved.\n")

            indented.render(CommentStyle.BLOCK) shouldBe header.render(CommentStyle.BLOCK)
        }
    }

    context("a file") {
        test("carries the header when it starts with it") {
            header.isPresentIn(header.render(CommentStyle.BLOCK) + "\npackage a\n", CommentStyle.BLOCK) shouldBe true
        }

        test("carries the header behind a byte order mark and with Windows line breaks") {
            val windows = header.render(CommentStyle.HASH).replace("\n", "\r\n")

            header.isPresentIn("﻿" + windows + "key=value\r\n", CommentStyle.HASH) shouldBe true
        }

        test("lacks the header when anything comes before it") {
            header.isPresentIn("\n" + header.render(CommentStyle.BLOCK), CommentStyle.BLOCK) shouldBe false
        }

        test("lacks the header when one line differs") {
            val altered = header.render(CommentStyle.BLOCK).replace("2026", "2025")

            header.isPresentIn(altered, CommentStyle.BLOCK) shouldBe false
        }
    }

    context("the notice") {
        test("is empty when it holds blank lines only") {
            LicenseHeader("\n   \n").isEmpty shouldBe true
            header.isEmpty shouldBe false
        }
    }

    context("the Apache License notice") {
        test("starts with the copyright line and names the license") {
            val notice = apacheLicenseNotice("  Copyright 2026 Davils ")

            notice shouldStartWith "Copyright 2026 Davils\n\nLicensed under the Apache License, Version 2.0"
            notice.lines().last() shouldBe "limitations under the License."
        }

        test("refuses a blank or multi-line copyright") {
            shouldThrow<IllegalArgumentException> { apacheLicenseNotice(" ") }
            shouldThrow<IllegalArgumentException> { apacheLicenseNotice("Copyright\n2026") }
        }
    }
})
