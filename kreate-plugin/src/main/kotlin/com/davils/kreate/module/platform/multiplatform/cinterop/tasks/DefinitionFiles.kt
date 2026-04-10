package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.CInteropExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

public abstract class GenerateDefinitionFiles : Task("Generates cinterop definition files for native targets") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @get:InputDirectory
    public abstract val rootDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

    @get:Input
    public abstract val cInteropConfig: Property<CInteropExtension>

    @TaskAction
    override fun execute() {
        val cinterop = validateCInteropDir()
        val definition = validateDefinitionFile(cinterop)
        writeFileContent(definition)
    }

    private fun validateCInteropDir(): File {
        val dir = workDir.get().asFile.resolve(cInteropConfig.get().defFiles.dirName.get())
        if (!dir.exists()) {
            try {
                dir.mkdirs()
            } catch (e: Exception) {
                logger.error("Failed to create cinterop directory.", e)
            }
        }
        return dir
    }

    private fun validateDefinitionFile(cinteropDir: File): File {
        val defFile = cinteropDir.resolve(cInteropConfig.get().defFiles.fileName.get())
        if (!defFile.exists()) {
            try {
                defFile.createNewFile()
            } catch (e: Exception) {
                logger.error("Failed to create cinterop definition file.", e)
            }
        }
        return defFile
    }

    private fun writeFileContent(defFile: File) {
        val rootDir = rootDir.get()
        val name = projectName.get()

        defFile.writeText(
            """
                headers = $name.h
                staticLibraries = lib$name.a
                compilerOpts = -I${rootDir.asFile.name}/${name}/include
                libraryPaths = ${rootDir.asFile.name}/${name}/target/x86_64-pc-windows-gnu/release ${rootDir.asFile.name}/${name}/target/x86_64-unknown-linux-gnu/release ${rootDir.asFile.name}/${name}/target/aarch64-unknown-linux-gnu/release ${rootDir.asFile.name}/${name}/target/x86_64-apple-darwin/release ${rootDir.asFile.name}/${name}/target/aarch64-apple-darwin/release 
            """.trimIndent()
        )
    }
}
