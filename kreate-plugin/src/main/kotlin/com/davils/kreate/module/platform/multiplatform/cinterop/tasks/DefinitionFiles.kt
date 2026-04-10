package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.CInteropExtension
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveRustTargets
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
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

    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    @TaskAction
    override fun execute() {
        val cinterop = validateCInteropDir()
        val definition = validateDefinitionFile(cinterop)
        writeFileContent(definition)
    }

    private fun validateCInteropDir(): File {
        val dir = workDir.get().asFile.resolve(cInteropConfig.get().defFiles.dirName.get())
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw GradleException("Failed to create cinterop directory: ${dir.absolutePath}")
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
                throw GradleException("Failed to create cinterop definition file: ${defFile.absolutePath}", e)
            }
        }
        return defFile
    }

    private fun writeFileContent(defFile: File) {
        val rootDir = rootDir.get()
        val name = projectName.get()
        val targets = resolveRustTargets(rustTargets)
        val libraryPaths = targets.joinToString(" ") { target ->
            "${rootDir.asFile.name}/${name}/target/${target}/release"
        }

        defFile.writeText(
            """
                headers = $name.h
                staticLibraries = lib$name.a
                compilerOpts = -I${rootDir.asFile.name}/${name}/include
                libraryPaths = $libraryPaths
            """.trimIndent()
        )
    }
}
