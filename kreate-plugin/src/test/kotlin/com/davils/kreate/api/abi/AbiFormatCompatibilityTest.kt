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
import io.kotest.core.test.Enabled
import io.kotest.matchers.shouldBe
import java.io.File

private const val CLASS_FILE_EXTENSION = "class"
private const val INTERNAL_API_MARKER = "com.davils.kreate.settings.InternalKreateApi"

class AbiFormatCompatibilityTest : FunSpec({
    val classesDirectory = File("build/classes/kotlin/main")
    val pluginWrittenDump = File("api/kreate-plugin.api")

    fun dumpInputsAvailability(): Enabled {
        if (!classesDirectory.isDirectory) return Enabled.disabled("Main classes have not been compiled yet.")
        if (!pluginWrittenDump.isFile) return Enabled.disabled("The checked-in dump is missing.")
        return Enabled.enabled
    }

    fun mainClassBytecode(): List<ByteArray> {
        val classFiles = classesDirectory.walkTopDown().filter { it.isFile && it.extension == CLASS_FILE_EXTENSION }
        val ordered = classFiles.sortedBy { it.invariantSeparatorsPath }
        return ordered.map { it.readBytes() }.toList()
    }

    context("BCV format compatibility") {
        test("reproduces the dump the binary-compatibility-validator plugin wrote")
            .config(enabledOrReasonIf = { dumpInputsAvailability() }) {
                val actual = AbiRenderer.render(
                    AbiExtractor.extract(
                        mainClassBytecode(),
                        AbiFilterOptions(nonPublicMarkers = setOf(INTERNAL_API_MARKER))
                    )
                )

                actual shouldBe readAbiDump(pluginWrittenDump)
            }
    }
})
