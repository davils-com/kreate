package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getOs
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class InitializeRustProject @Inject constructor(
    private val exec: ExecOperations
) : Task("Generates a new rust project.") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @get:Input
    public abstract val projectName: Property<String>

    @TaskAction
    override fun execute() {
        val workDirFile = workDir.get().asFile
        val projectName = projectName.get()
        val rustProjectDir = workDirFile.resolve(projectName)

        if (rustProjectDir.exists()) {
            return
        }

        val os by getOs()
        try {
            when(os) {
                OsTarget.MACOS -> {
                    exec.exec {
                        workingDir = workDirFile
                        commandLine("${System.getProperty("user.home")}/.cargo/bin/cargo", "new", "--lib", projectName)
                    }
                }
                else -> {
                    exec.exec {
                        workingDir = workDirFile
                        commandLine("cargo", "new", "--lib", projectName)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize rust project.", e)
        }
    }
}