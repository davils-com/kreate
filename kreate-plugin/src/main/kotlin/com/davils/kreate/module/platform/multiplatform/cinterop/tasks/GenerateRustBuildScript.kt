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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to generate the `build.rs` script for the Rust project.
 *
 * This task creates a build script that uses `cbindgen` to generate C headers
 * from Rust source code during the Cargo build process.
 *
 * @since 1.0.0
 */
public abstract class GenerateRustBuildScript : Task(
    "Generates the build script for the Rust project.",
    "kreate c-interoperation"
) {
    /**
     * The working directory containing the Rust project.
     * @since 1.0.0
     */
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    /**
     * The name of the Rust project.
     * @since 1.0.0
     */
    @get:Input
    public abstract val projectName: Property<String>

    /**
     * The content of the `build.rs` script.
     * @since 1.0.0
     */
    private val script: String
        get() = """
            extern crate cbindgen;

            use std::env;
            use cbindgen::Language::C;

            fn main() {
                let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();

                cbindgen::Builder::new()
                    .with_crate(crate_dir)
                    .with_language(C)
                    .generate()
                    .expect("Unable to generate bindings")
                    .write_to_file("include/${projectName.get()}.h");
            }
        """.trimIndent()

    /**
     * The output file representing the generated `build.rs`.
     * @since 1.0.0
     */
    @get:OutputFile
    public val outputFile: File
        get() = workDir.get().asFile.resolve(BUILD_RUST_FILE_NAME)

    /**
     * Executes the task to generate the build script.
     *
     * @since 1.0.0
     */
    @TaskAction
    override fun execute() {
        val buildRsFile = workDir.get().asFile.resolve(BUILD_RUST_FILE_NAME)
        if (!buildRsFile.exists()) {
            buildRsFile.createNewFile()
            buildRsFile.writeText(script)
            return
        }

        if (!isFileEmpty(buildRsFile)) {
            return
        }
        buildRsFile.writeText(script)
    }

    /**
     * Checks if the given file is empty.
     *
     * @param buildRsFile The file to check.
     * @return `true` if empty, `false` otherwise.
     * @since 1.0.0
     */
    private fun isFileEmpty(buildRsFile: File): Boolean {
        return buildRsFile.length() == 0L
    }

    /**
     * Companion object for [GenerateRustBuildScript].
     * @since 1.0.0
     */
    public companion object {
        /**
         * The name of the Rust build script file.
         * @since 1.0.0
         */
        public const val BUILD_RUST_FILE_NAME: String = "build.rs"
    }
}
