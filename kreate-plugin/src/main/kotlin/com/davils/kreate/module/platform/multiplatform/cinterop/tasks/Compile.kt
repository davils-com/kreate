package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.system.Architecture
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getArchitecture
import com.davils.kreate.system.getOs
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class CompileRust @Inject constructor(
    private val exec: ExecOperations
) : Task("Compile Rust code for C interop") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @TaskAction
    override fun execute() {
        val arch by getArchitecture()
        val os by getOs()

        try {
            when(os) {
                OsTarget.WINDOWS -> {
                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("cargo", "build", "--target", "x86_64-pc-windows-gnu", "--release")
                    }
                }

                OsTarget.LINUX -> {
                    when(arch) {
                        Architecture.X64 -> {
                            exec.exec {
                                workingDir = workDir.get().asFile
                                commandLine("cargo", "build", "--target", "x86_64-unknown-linux-gnu", "--release")
                            }
                        }
                        else -> {
                            exec.exec {
                                workingDir = workDir.get().asFile
                                commandLine("cargo", "build", "--target", "aarch64-unknown-linux-gnu", "--release")
                            }
                        }
                    }
                }

                OsTarget.MACOS -> {
                    exec.exec {
                        workingDir = workDir.get().asFile
                        commandLine("${System.getProperty("user.home")}/.cargo/bin/cargo", "build", "--target", "aarch64-apple-darwin", "--release")
                    }
                }

                else -> throw IllegalStateException("Unsupported OS: $os")
            }
        } catch (e: Exception) {
            logger.error("Failed to compile Rust code.", e)
        }
    }
}
