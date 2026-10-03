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

package com.davils.kreate.cinterop.task

import com.davils.kreate.task.KreateTask
import com.davils.kreate.task.KreateTaskGroup
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

internal const val CARGO_TOML_FILE_NAME: String = "Cargo.toml"

@DisableCachingByDefault(because = "Cargo configuration has side effects on Cargo.toml")
internal abstract class ConfigureCargo : KreateTask(
    "Configure Cargo Toml file for static library output.",
    KreateTaskGroup.C_INTEROP
) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Input
    public val extendedCargoContent: String
        get() = """
            [lib]
            crate-type = ["staticlib"]
        """.trimIndent()

    @get:OutputFile
    public val outputFile: File
        get() = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)

    @TaskAction
    public fun execute() {
        val cargoToml = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)
        if (!isValidCargoToml(cargoToml)) {
            return
        }

        if (isContentAlreadyExtended(cargoToml)) {
            return
        }
        writeContentToFile(cargoToml)
    }

    private fun isValidCargoToml(cargoToml: File): Boolean = cargoToml.exists()

    private fun isContentAlreadyExtended(cargoToml: File): Boolean {
        val content = cargoToml.readText()
        return content.contains(extendedCargoContent)
    }

    private fun writeContentToFile(cargoToml: File) {
        val originalContent = cargoToml.readText()
        val newContent = originalContent + extendedCargoContent
        cargoToml.writeText(newContent)
    }
}
