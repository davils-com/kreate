package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

public abstract class GenerateRustBuildScript : Task("Generates the build script for the Rust project.") {
    @get:InputDirectory
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

    private fun isFileEmpty(buildRsFile: File): Boolean {
        return buildRsFile.length() == 0L
    }

    public companion object {
        public const val BUILD_RUST_FILE_NAME: String = "build.rs"
    }
}
