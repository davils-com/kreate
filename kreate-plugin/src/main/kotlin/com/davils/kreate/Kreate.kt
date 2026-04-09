package com.davils.kreate

import com.davils.kreate.module.builder.modules
import com.davils.kreate.module.platform.PlatformModule
import com.davils.kreate.module.project.ProjectModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

public class Kreate : Plugin<Project> {
    override fun apply(project: Project) {
        val kreateExtension = project.extensions.create<KreateExtension>("kreate")
        project.addModules(kreateExtension)
    }
}

private fun Project.addModules(extension: KreateExtension) {
    val modules = modules(extension) {
        add(ProjectModule)
        add(PlatformModule)
    }
    modules.applyAll()
}