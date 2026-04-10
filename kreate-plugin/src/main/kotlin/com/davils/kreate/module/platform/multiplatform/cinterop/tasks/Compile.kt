package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveCargoCommand
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveRustTargets
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class CompileRust @Inject constructor(
    private val exec: ExecOperations
) : Task("Compile Rust code for C interop") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val rustTargets: ListProperty<String>

    @TaskAction
    override fun execute() {
        val targets = resolveRustTargets(rustTargets)
        val cargoCmd = resolveCargoCommand()

        for (target in targets) {
            try {
                exec.exec {
                    workingDir = workDir.get().asFile
                    commandLine(cargoCmd, "build", "--target", target, "--release")
                }
            } catch (e: Exception) {
                throw GradleException("Failed to compile Rust code for target '$target'.", e)
            }
        }
    }
}
