package com.davils.kreate.module.platform.multiplatform.cinterop.tasks

import com.davils.kreate.jobs.Task
import com.davils.kreate.module.platform.multiplatform.cinterop.resolveCargoCommand
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

public abstract class AddRustDependencies @Inject constructor(
    private val exec: ExecOperations
) : Task("Adds rust dependencies to the project.") {
    @get:InputDirectory
    public abstract val workDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val rustDependencies: MapProperty<String, String>

    @get:Input
    @get:Optional
    public abstract val rustBuildDependencies: MapProperty<String, String>

    @TaskAction
    override fun execute() {
        val cargoToml = workDir.get().asFile.resolve("Cargo.toml")
        if (!cargoToml.exists()) {
            throw GradleException("Cargo.toml not found in ${workDir.get().asFile.absolutePath}")
        }
        val cargoContent = cargoToml.readText()

        val deps = if (rustDependencies.isPresent && rustDependencies.get().isNotEmpty()) {
            rustDependencies.get()
        } else {
            mapOf("libc" to "")
        }

        val buildDeps = if (rustBuildDependencies.isPresent && rustBuildDependencies.get().isNotEmpty()) {
            rustBuildDependencies.get()
        } else {
            mapOf("cbindgen" to "")
        }

        val cargoCmd = resolveCargoCommand()

        for ((name, version) in deps) {
            if (isDependencyPresent(cargoContent, name)) continue
            addDependency(cargoCmd, name, version, build = false)
        }

        for ((name, version) in buildDeps) {
            if (isDependencyPresent(cargoContent, name)) continue
            addDependency(cargoCmd, name, version, build = true)
        }
    }

    private fun isDependencyPresent(cargoContent: String, name: String): Boolean {
        return cargoContent.contains(name)
    }

    private fun addDependency(cargoCmd: String, name: String, version: String, build: Boolean) {
        try {
            exec.exec {
                workingDir = workDir.get().asFile
                val cmd = mutableListOf(cargoCmd, "add")
                if (build) cmd.add("--build")
                if (version.isNotEmpty()) cmd.add("$name@$version") else cmd.add(name)
                commandLine(cmd)
            }
        } catch (e: Exception) {
            throw GradleException("Failed to add rust dependency '$name'.", e)
        }
    }
}
