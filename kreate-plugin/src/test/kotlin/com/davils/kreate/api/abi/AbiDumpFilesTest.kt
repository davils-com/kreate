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

import com.davils.kreate.freshDirectory
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.io.File

private const val LF_DUMP = "public final class com/example/Sample {\n\tpublic fun <init> ()V\n}\n\n"

class AbiDumpFilesTest : FunSpec({
    val workspace = tempdir()

    fun dumpFile(content: String): File {
        val file = File(workspace.freshDirectory(), "sample.api")
        file.writeText(content)
        return file
    }

    fun crlfDump(): String = LF_DUMP.replace("\n", "\r\n")

    context("ABI dump reading") {
        test("leaves a dump written with line feeds alone") {
            readAbiDump(dumpFile(LF_DUMP)) shouldBe LF_DUMP
        }

        test("accepts a dump Git checked out with carriage returns") {
            readAbiDump(dumpFile(crlfDump())) shouldBe LF_DUMP
        }

        test("reports no difference against a dump that only differs in its line endings") {
            val expected = readAbiDump(dumpFile(crlfDump()))

            AbiDiff.render(expected = expected, actual = LF_DUMP) shouldBe null
        }
    }
})
