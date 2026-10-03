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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

internal const val BUILD_RUST_FILE_NAME: String = "build.rs"

@DisableCachingByDefault(because = "Build script generation is conditional and depends on external state")
internal abstract class GenerateRustBuildScript : KreateTask(
    "Generates the build script for the Rust project.",
    KreateTaskGroup.C_INTEROP
) {
    @get:Internal
    public abstract val workDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

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

    @get:OutputFile
    public val outputFile: File
        get() = workDir.get().asFile.resolve(BUILD_RUST_FILE_NAME)

    @TaskAction
    public fun execute() {
        val buildRsFile = workDir.get().asFile.resolve(BUILD_RUST_FILE_NAME)
        if (hasContent(buildRsFile)) return

        buildRsFile.writeText(script)
    }

    private fun hasContent(buildRsFile: File): Boolean = buildRsFile.exists() && buildRsFile.length() > 0L
}
