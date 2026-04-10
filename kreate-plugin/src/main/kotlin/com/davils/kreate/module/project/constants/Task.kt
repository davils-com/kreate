package com.davils.kreate.module.project.constants

import com.davils.kreate.KreateExtension
import com.davils.kreate.jobs.Task
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class GenerateBuildConstantsTask : Task("Generates build constants as a Kotlin file.") {
    @get:Input
    public abstract val properties: MapProperty<String, String>

    @get:Input
    public abstract val packageName: Property<String>

    @get:Input
    public abstract val className: Property<String>

    @get:Input
    public abstract val explicitApi: Property<Boolean>

    @get:OutputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val file: RegularFileProperty

    @TaskAction
    override fun execute() {
        val props = properties.get()
    }
}

internal fun Project.registerBuildConstantsTask(extension: KreateExtension) {

}