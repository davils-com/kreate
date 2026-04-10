package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveCargoCommand
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class AddRustDependencies @Inject constructor(
    private val exec: ExecOperations
) : Task("Adds rust dependencies to the project.") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @TaskAction
    override fun execute() {
        val cargoCmd = resolveCargoCommand()
        try {
            exec.exec {
                workingDir = workDir.get().asFile
                commandLine(cargoCmd, "add", "libc")
            }

            exec.exec {
                workingDir = workDir.get().asFile
                commandLine(cargoCmd, "add", "--build", "cbindgen")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to add rust dependencies.", e)
        }
    }
}
