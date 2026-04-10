package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getOs
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
        val os by getOs()
        try {
            when(os) {
                OsTarget.MACOS -> {
                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("${System.getProperty("user.home")}/.cargo/bin/cargo", "add", "libc")
                    }

                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("${System.getProperty("user.home")}/.cargo/bin/cargo", "add", "--build", "cbindgen")
                    }
                }

                else -> {
                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("cargo", "add", "libc")
                    }

                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("cargo", "add", "--build", "cbindgen")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to add rust dependencies.", e)
        }
    }
}
