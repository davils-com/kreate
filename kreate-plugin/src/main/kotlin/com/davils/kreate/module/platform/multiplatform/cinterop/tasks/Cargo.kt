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

/**
 * Task to configure the Cargo.toml file for static library output.
 *
 * This task appends the necessary `[lib]` configuration to `Cargo.toml` if it's
 * not already present, ensuring that the Rust project can be built as a
 * static library for C-interop.
 *
 * @since 1.0.0
 */
public abstract class ConfigureCargo : Task("Configure Cargo Toml file for static library output.", "kreate c-interoperation") {
    /**
     * The working directory containing the `Cargo.toml` file.
     * @since 1.0.0
     */
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    /**
     * The content to append to `Cargo.toml`.
     * @since 1.0.0
     */
    private val extendedCargoContent: String
        get() = """
            [lib]
            crate-type = ["staticlib"]
        """.trimIndent()

    /**
     * The output file representing the configured `Cargo.toml`.
     * @since 1.0.0
     */
    @get:OutputFile
    public val outputFile: File
        get() = workDir.get().asFile.resolve(CARGO_TOML_FILE_NAME)

    /**
     * Executes the task to configure `Cargo.toml`.
     *
     * @since 1.0.0
     */
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

    /**
     * Checks if the `Cargo.toml` file is valid and exists.
     *
     * @param cargoToml The file to check.
     * @return `true` if it exists, `false` otherwise.
     * @since 1.0.0
     */
    private fun isValidCargoToml(cargoToml: File): Boolean {
        return cargoToml.exists()
    }

    /**
     * Checks if the `Cargo.toml` already contains the extended content.
     *
     * @param cargoToml The file to check.
     * @return `true` if already extended, `false` otherwise.
     * @since 1.0.0
     */
    private fun isContentAlreadyExtended(cargoToml: File): Boolean {
        val content = cargoToml.readText()
        return content.contains(extendedCargoContent)
    }

    /**
     * Writes the extended content to the `Cargo.toml` file.
     *
     * @param cargoToml The file to write to.
     * @since 1.0.0
     */
    private fun writeContentToFile(cargoToml: File) {
        val originalContent = cargoToml.readText()
        val newContent = originalContent + extendedCargoContent
        cargoToml.writeText(newContent)
    }

    /**
     * Companion object for [ConfigureCargo].
     * @since 1.0.0
     */
    public companion object {
        /**
         * The name of the Cargo configuration file.
         * @since 1.0.0
         */
        public const val CARGO_TOML_FILE_NAME: String = "Cargo.toml"
    }
}
