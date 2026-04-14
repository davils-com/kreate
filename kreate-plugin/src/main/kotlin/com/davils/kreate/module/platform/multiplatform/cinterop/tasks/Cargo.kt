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

package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

public abstract class ConfigureCargo : Task("Configure Cargo Toml file for static library output.") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    private val extendedCargoContent: String
        get() = """
            [lib]
            crate-type = ["staticlib"]
        """.trimIndent()

    @get:OutputFile
    public val outputFile: File
        get() = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)

    @TaskAction
    override fun execute() {
        val cargoToml = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)
        if (!isValidCargoToml(cargoToml)) {
            return
        }

        if (isContentAlreadyExtended(cargoToml)) {
            return
        }
        writeContentToFile(cargoToml)
    }

    private fun isValidCargoToml(cargoToml: File): Boolean {
        return cargoToml.exists()
    }

    private fun isContentAlreadyExtended(cargoToml: File): Boolean {
        val content = cargoToml.readText()
        return content.contains(extendedCargoContent)
    }

    private fun writeContentToFile(cargoToml: File) {
        val originalContent = cargoToml.readText()
        val newContent = originalContent + extendedCargoContent
        cargoToml.writeText(newContent)
    }

    public companion object {
        public const val CARGO_TOML_FILE_NAME: String = "Cargo.toml"
    }
}
